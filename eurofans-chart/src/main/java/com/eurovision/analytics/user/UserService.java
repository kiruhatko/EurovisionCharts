package com.eurovision.analytics.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User findOrCreate(long telegramId, String telegramUsername, String firstName) {
        return userRepository.findByTelegramId(telegramId)
                .map(existing -> {
                    boolean changed = false;
                    if (!java.util.Objects.equals(existing.getTelegramUsername(), telegramUsername)) {
                        existing.setTelegramUsername(telegramUsername);
                        changed = true;
                    }
                    if (!java.util.Objects.equals(existing.getFirstName(), firstName)) {
                        existing.setFirstName(firstName);
                        changed = true;
                    }
                    if (changed) {
                        existing.touch();
                    }
                    return existing;
                })
                .orElseGet(() -> userRepository.save(new User(telegramId, telegramUsername, firstName)));
    }

    /** /logout — global tracking + chart participation off, history retained. */
    @Transactional
    public void logout(User user) {
        user.setTrackingEnabled(false);
        user.setChartParticipationEnabled(false);
        user.touch();
    }
}
