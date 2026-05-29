package sk.tuke.gamestudio.service;

import sk.tuke.gamestudio.entity.PlayerAccount;

public interface AuthService {

    PlayerAccount register(String username, String password);

    PlayerAccount login(String username, String password);

    PlayerAccount getAccount(String username);
}
