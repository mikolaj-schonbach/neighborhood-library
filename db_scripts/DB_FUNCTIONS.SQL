-- ============================================================
-- Helpers
-- ============================================================

CREATE OR REPLACE FUNCTION fn_assert_user_active(p_user_id BIGINT)
RETURNS VOID
LANGUAGE plpgsql
AS $$
DECLARE
  v_status TEXT;
BEGIN
  SELECT status INTO v_status
  FROM users
  WHERE id = p_user_id;

  IF v_status IS NULL THEN
    RAISE EXCEPTION 'User % does not exist', p_user_id;
  END IF;

  IF v_status <> 'ACTIVE' THEN
    RAISE EXCEPTION 'User % is not ACTIVE (status=%)', p_user_id, v_status;
  END IF;
END;
$$;


CREATE OR REPLACE FUNCTION fn_check_limit_3(p_user_id BIGINT, p_add INT DEFAULT 0)
RETURNS VOID
LANGUAGE plpgsql
AS $$
DECLARE
  v_active_res   INT;
  v_active_loans INT;
  v_total        INT;
BEGIN
  SELECT COUNT(*) INTO v_active_res
  FROM reservations
  WHERE user_id = p_user_id AND status = 'ACTIVE';

  SELECT COUNT(*) INTO v_active_loans
  FROM loans
  WHERE user_id = p_user_id AND returned_at IS NULL;

  v_total := v_active_res + v_active_loans + p_add;

  IF v_total > 3 THEN
    RAISE EXCEPTION
      'Limit exceeded: user % would have % active items (res=% loans=% add=%)',
      p_user_id, v_total, v_active_res, v_active_loans, p_add;
  END IF;
END;
$$;



-- Recompute derived status for a copy (unless it is UNAVAILABLE override)
CREATE OR REPLACE FUNCTION fn_recompute_copy_status(p_copy_id BIGINT)
RETURNS VOID
LANGUAGE plpgsql
AS $$
DECLARE
  v_current TEXT;
  v_has_active_loan BOOLEAN;
  v_has_active_res BOOLEAN;
  v_new TEXT;
BEGIN
  SELECT status INTO v_current
  FROM copies
  WHERE id = p_copy_id
  FOR UPDATE;

  IF v_current IS NULL THEN
    -- copy deleted or not found; nothing to do
    RETURN;
  END IF;

  -- UNAVAILABLE is a manual override with priority over everything
  IF v_current = 'UNAVAILABLE' THEN
    RETURN;
  END IF;

  SELECT EXISTS(
    SELECT 1 FROM loans
    WHERE copy_id = p_copy_id
      AND returned_at IS NULL
  ) INTO v_has_active_loan;

  IF v_has_active_loan THEN
    v_new := 'LOANED';
  ELSE
    SELECT EXISTS(
      SELECT 1 FROM reservations
      WHERE copy_id = p_copy_id
        AND status = 'ACTIVE'
    ) INTO v_has_active_res;

    IF v_has_active_res THEN
      v_new := 'RESERVED';
    ELSE
      v_new := 'AVAILABLE';
    END IF;
  END IF;

  IF v_new <> v_current THEN
    UPDATE copies
    SET status = v_new
    WHERE id = p_copy_id;
  END IF;
END;
$$;


-- ============================================================
-- copies: manual UNAVAILABLE override + block other manual edits
-- ============================================================

CREATE OR REPLACE FUNCTION trg_copies_before_update_status()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
  v_has_active_res BOOLEAN;
  v_has_active_loan BOOLEAN;
BEGIN
-- pozwól na wewnętrzne aktualizacje statusu wykonywane z innych triggerów
IF NEW.status IS DISTINCT FROM OLD.status AND pg_trigger_depth() > 1 THEN
  RETURN NEW;
END IF;


  IF NEW.status IS DISTINCT FROM OLD.status THEN

    -- Manual set to UNAVAILABLE is allowed only if no active reservation/loan exists
    IF NEW.status = 'UNAVAILABLE' THEN
      SELECT EXISTS(
        SELECT 1 FROM reservations
        WHERE copy_id = OLD.id AND status = 'ACTIVE'
      ) INTO v_has_active_res;

      SELECT EXISTS(
        SELECT 1 FROM loans
        WHERE copy_id = OLD.id AND returned_at IS NULL
      ) INTO v_has_active_loan;

      IF v_has_active_res OR v_has_active_loan THEN
        RAISE EXCEPTION 'Cannot set copy % to UNAVAILABLE: active reservation/loan exists', OLD.id;
      END IF;

      RETURN NEW;
    END IF;

    -- "Un-override": allow changing status away from UNAVAILABLE,
    -- but final status will be recomputed by AFTER trigger
    IF OLD.status = 'UNAVAILABLE' THEN
      RETURN NEW;
    END IF;

    -- Otherwise: status is managed by triggers and cannot be manually changed
    RAISE EXCEPTION 'copies.status is managed by DB triggers (allowed manual: set to UNAVAILABLE or unset from UNAVAILABLE)';
  END IF;

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS copies_before_update_status ON copies;
CREATE TRIGGER copies_before_update_status
BEFORE UPDATE OF status ON copies
FOR EACH ROW
EXECUTE FUNCTION trg_copies_before_update_status();


CREATE OR REPLACE FUNCTION trg_copies_after_update_status()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
  -- If UNAVAILABLE was removed, recompute derived status
  IF OLD.status = 'UNAVAILABLE' AND NEW.status <> 'UNAVAILABLE' THEN
    PERFORM fn_recompute_copy_status(NEW.id);
  END IF;

  RETURN NULL;
END;
$$;

DROP TRIGGER IF EXISTS copies_after_update_status ON copies;
CREATE TRIGGER copies_after_update_status
AFTER UPDATE OF status ON copies
FOR EACH ROW
EXECUTE FUNCTION trg_copies_after_update_status();


-- ============================================================
-- reservations: enforce user ACTIVE, limit 3, and recompute copy status
-- ============================================================

CREATE OR REPLACE FUNCTION trg_reservations_before_insert()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
  v_copy_status TEXT;
BEGIN
  -- Prevent race conditions per user
  PERFORM pg_advisory_xact_lock(NEW.user_id);

  -- Only ACTIVE users can create reservations
  PERFORM fn_assert_user_active(NEW.user_id);

  -- Enforce global limit 3 (active reservations + active loans)
  PERFORM fn_check_limit_3(NEW.user_id, 1);

  -- Cannot reserve an UNAVAILABLE copy, and typically you want AVAILABLE only
  SELECT status INTO v_copy_status
  FROM copies
  WHERE id = NEW.copy_id
  FOR UPDATE;

  IF v_copy_status IS NULL THEN
    RAISE EXCEPTION 'Copy % does not exist', NEW.copy_id;
  END IF;

  IF v_copy_status <> 'AVAILABLE' THEN
    RAISE EXCEPTION 'Cannot create reservation: copy % is not AVAILABLE (status=%)', NEW.copy_id, v_copy_status;
  END IF;

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS reservations_before_insert ON reservations;
CREATE TRIGGER reservations_before_insert
BEFORE INSERT ON reservations
FOR EACH ROW
EXECUTE FUNCTION trg_reservations_before_insert();

-- Block changing reservations.copy_id (no "moving reservations" between copies)
CREATE OR REPLACE FUNCTION trg_reservations_before_update_copy_id()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
  IF NEW.copy_id IS DISTINCT FROM OLD.copy_id THEN
    RAISE EXCEPTION
      'Changing reservations.copy_id is not allowed (reservation_id=%; old_copy_id=%; new_copy_id=%)',
      OLD.id, OLD.copy_id, NEW.copy_id;
  END IF;

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS reservations_before_update_copy_id ON reservations;
CREATE TRIGGER reservations_before_update_copy_id
BEFORE UPDATE OF copy_id ON reservations
FOR EACH ROW
EXECUTE FUNCTION trg_reservations_before_update_copy_id();



CREATE OR REPLACE FUNCTION trg_reservations_after_change()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
  v_copy_id BIGINT;
BEGIN
  v_copy_id := COALESCE(NEW.copy_id, OLD.copy_id);
  IF v_copy_id IS NOT NULL THEN
    PERFORM fn_recompute_copy_status(v_copy_id);
  END IF;
  RETURN NULL;
END;
$$;

DROP TRIGGER IF EXISTS reservations_after_insert ON reservations;
CREATE TRIGGER reservations_after_insert
AFTER INSERT ON reservations
FOR EACH ROW
EXECUTE FUNCTION trg_reservations_after_change();

DROP TRIGGER IF EXISTS reservations_after_update ON reservations;
CREATE TRIGGER reservations_after_update
AFTER UPDATE OF status, cancelled_at, expired_at, fulfilled_at, copy_id ON reservations
FOR EACH ROW
EXECUTE FUNCTION trg_reservations_after_change();

DROP TRIGGER IF EXISTS reservations_after_delete ON reservations;
CREATE TRIGGER reservations_after_delete
AFTER DELETE ON reservations
FOR EACH ROW
EXECUTE FUNCTION trg_reservations_after_change();


-- ============================================================
-- loans: enforce reservation ACTIVE + pickup_until, fulfill reservation, recompute copy status
-- ============================================================

CREATE OR REPLACE FUNCTION trg_loans_before_insert()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
  v_r_status TEXT;
  v_pickup_until TIMESTAMPTZ;
  v_now TIMESTAMPTZ := now();
  v_copy_status TEXT;
BEGIN
  -- Prevent race conditions per user
  PERFORM pg_advisory_xact_lock(NEW.user_id);

  -- Only ACTIVE users can have a new loan created
  PERFORM fn_assert_user_active(NEW.user_id);

  -- Limit 3: allow loan even if user currently has 3 active items,
  -- because this loan will consume one ACTIVE reservation (net doesn't increase).
  -- So we enforce only that the user is not already above the limit.
  PERFORM fn_check_limit_3(NEW.user_id, 0);

  -- Lock reservation row and validate it
  SELECT status, pickup_until INTO v_r_status, v_pickup_until
  FROM reservations
  WHERE id = NEW.reservation_id
    AND copy_id = NEW.copy_id
    AND user_id = NEW.user_id
  FOR UPDATE;

  IF v_r_status IS NULL THEN
    RAISE EXCEPTION 'Reservation % not found for (user %, copy %)', NEW.reservation_id, NEW.user_id, NEW.copy_id;
  END IF;

  -- If pickup window already passed, expire it and block loan
IF v_r_status = 'ACTIVE' AND v_now > v_pickup_until THEN
  UPDATE reservations
  SET status = 'EXPIRED',
      expired_at = COALESCE(expired_at, v_now)
  WHERE id = NEW.reservation_id;

  RAISE NOTICE 'Loan not created: reservation % expired (pickup_until=% now=%)',
    NEW.reservation_id, v_pickup_until, v_now;

  RETURN NULL;  -- klucz: brak wyjątku => UPDATE się utrzyma
END IF;


  IF v_r_status <> 'ACTIVE' THEN
    RAISE EXCEPTION 'Cannot create loan: reservation % is not ACTIVE (status=%)', NEW.reservation_id, v_r_status;
  END IF;

  -- copy cannot be UNAVAILABLE at this moment
  SELECT status INTO v_copy_status
  FROM copies
  WHERE id = NEW.copy_id
  FOR UPDATE;

  IF v_copy_status = 'UNAVAILABLE' THEN
    RAISE EXCEPTION 'Cannot create loan: copy % is UNAVAILABLE', NEW.copy_id;
  END IF;

  -- Fulfill reservation
  UPDATE reservations
  SET status = 'FULFILLED',
      fulfilled_at = COALESCE(fulfilled_at, v_now)
  WHERE id = NEW.reservation_id;

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS loans_before_insert ON loans;
CREATE TRIGGER loans_before_insert
BEFORE INSERT ON loans
FOR EACH ROW
EXECUTE FUNCTION trg_loans_before_insert();


CREATE OR REPLACE FUNCTION trg_loans_after_change()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
  v_copy_id BIGINT;
BEGIN
  v_copy_id := COALESCE(NEW.copy_id, OLD.copy_id);

  IF v_copy_id IS NOT NULL THEN
    PERFORM fn_recompute_copy_status(v_copy_id);
  END IF;

  RETURN NULL;
END;
$$;

DROP TRIGGER IF EXISTS loans_after_insert ON loans;
CREATE TRIGGER loans_after_insert
AFTER INSERT ON loans
FOR EACH ROW
EXECUTE FUNCTION trg_loans_after_change();

-- Recompute when returned_at changes (return)
DROP TRIGGER IF EXISTS loans_after_update_returned ON loans;
CREATE TRIGGER loans_after_update_returned
AFTER UPDATE OF returned_at ON loans
FOR EACH ROW
EXECUTE FUNCTION trg_loans_after_change();

DROP TRIGGER IF EXISTS loans_after_delete ON loans;
CREATE TRIGGER loans_after_delete
AFTER DELETE ON loans
FOR EACH ROW
EXECUTE FUNCTION trg_loans_after_change();
