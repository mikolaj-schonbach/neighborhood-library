package com.example.neighborhood_library.config;

import com.example.neighborhood_library.repo.UserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class LibraryUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public LibraryUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        return userRepository.findByLogin(login)
                .map(LibraryUserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("Nieprawidłowy login lub hasło."));
    }
}
