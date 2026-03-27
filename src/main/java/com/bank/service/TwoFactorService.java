package com.bank.service;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import com.bank.domain.User;
import com.bank.exception.UserNotFoundException;
import com.bank.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TwoFactorService {

    @Autowired
    private UserRepository userRepository;

    private final GoogleAuthenticator gAuth = new GoogleAuthenticator();

    @Transactional
    public String setupTwoFactor(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));

        GoogleAuthenticatorKey key = gAuth.createCredentials();
        user.setTotpSecret(key.getKey());
        user.setTwoFactorEnabled(false); // Not enabled until verified
        userRepository.save(user);

        return GoogleAuthenticatorQRGenerator.getOtpAuthTotpURL(
                "SecureBank", username, key);
    }

    @Transactional
    public boolean verifyAndEnable(String username, int code) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));

        if (user.getTotpSecret() == null) {
            throw new IllegalStateException("2FA not set up for this user");
        }

        boolean valid = gAuth.authorize(user.getTotpSecret(), code);
        if (valid) {
            user.setTwoFactorEnabled(true);
            userRepository.save(user);
        }
        return valid;
    }

    @Transactional
    public boolean disableTwoFactor(String username, int code) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));

        boolean valid = gAuth.authorize(user.getTotpSecret(), code);
        if (valid) {
            user.setTwoFactorEnabled(false);
            user.setTotpSecret(null);
            userRepository.save(user);
        }
        return valid;
    }

    public boolean validateCode(String username, int code) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));

        if (!user.isTwoFactorEnabled() || user.getTotpSecret() == null) {
            return true; // 2FA not enabled, skip validation
        }
        return gAuth.authorize(user.getTotpSecret(), code);
    }

    public boolean isTwoFactorEnabled(String username) {
        return userRepository.findByUsername(username)
                .map(User::isTwoFactorEnabled)
                .orElse(false);
    }
}
