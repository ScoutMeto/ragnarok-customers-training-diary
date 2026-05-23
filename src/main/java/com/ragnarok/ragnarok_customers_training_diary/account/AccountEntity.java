package com.ragnarok.ragnarok_customers_training_diary.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Sjednocený model uživatele aplikace. Role (USER vs ADMIN) rozhoduje o oprávněních.
 *
 * <p>Nahrazuje původní oddělené {@code UserEntity} a {@code AdminEntity}, které vedly k duplicitě
 * kódu a konfliktům v Spring Security konfiguraci.
 */
@Entity
@Table(name = "account")
@Getter
@Setter
@NoArgsConstructor
public class AccountEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountRole role;

    @Column(nullable = false)
    private String nickname;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    private String phone;

    @Column(name = "email_notifications_enabled", nullable = false)
    private boolean emailNotificationsEnabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Soft-delete timestamp. {@code null} = aktivní účet. Když je nastaven, účet
     * je anonymizován (email, jméno, telefon přepsány) a v dotazech aktivních
     * účtů se ignoruje.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /** Konvenience getter — `deletedAt == null`. */
    public boolean isActive() {
        return deletedAt == null;
    }

    // ---------------------------------------------------------------------------------------------
    // UserDetails (Spring Security)
    // ---------------------------------------------------------------------------------------------

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** Soft-deleted účet se nemůže přihlásit. */
    @Override
    public boolean isEnabled() {
        return deletedAt == null;
    }
}
