package com.cursosdedesarrollo.websockettwitchchat.auth;

import com.cursosdedesarrollo.websockettwitchchat.repositories.CreatorAccountEntity;
import com.cursosdedesarrollo.websockettwitchchat.repositories.CreatorAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import java.lang.reflect.Field;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CreatorAccountSecurityConfigTest {
    @Test void onlyVerifiedCreatorCanBeLoadedForAuthentication() throws Exception {
        CreatorAccountRepository accounts=mock(CreatorAccountRepository.class);
        CreatorAccountSecurityConfig config=new CreatorAccountSecurityConfig();
        var userDetails=config.creatorUsers(accounts);
        CreatorAccountEntity unverified=account(false);
        when(accounts.findByEmail("creator@example.test")).thenReturn(Optional.of(unverified));
        assertThrows(UsernameNotFoundException.class,()->userDetails.loadUserByUsername("creator@example.test"));
        CreatorAccountEntity verified=account(true);
        when(accounts.findByEmail("creator@example.test")).thenReturn(Optional.of(verified));
        var user=userDetails.loadUserByUsername("creator@example.test");
        assertEquals("creator@example.test",user.getUsername());
        assertEquals("argon2-hash",user.getPassword());
        assertTrue(user.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_CREATOR")));
    }
    private static CreatorAccountEntity account(boolean verified) throws Exception {
        CreatorAccountEntity entity=new CreatorAccountEntity("creator@example.test","argon2-hash",1L);
        Field field=CreatorAccountEntity.class.getDeclaredField("emailVerified"); field.setAccessible(true); field.set(entity,verified);
        return entity;
    }
}
