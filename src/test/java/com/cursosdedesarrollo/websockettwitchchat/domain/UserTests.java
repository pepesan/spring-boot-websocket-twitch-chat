package com.cursosdedesarrollo.websockettwitchchat.domain;

import com.cursosdedesarrollo.websockettwitchchat.domain.twitch.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UserTests {
    @Test
    public void checkConstructor(){
        User user = new User();
        assertEquals(user.getId(),"");
        assertEquals(user.getLogin(),"");
        assertEquals(user.getDisplayName(),"");
        assertEquals(user.getType(),"");
        assertEquals(user.getBroadcasterType(),"");
        assertEquals(user.getDescription(),"");
        assertEquals(user.getProfileImageUrl(),"");
        assertEquals(user.getOfflineImageUrl(),"");
        assertEquals(user.getEmail(),"");
    }
}
