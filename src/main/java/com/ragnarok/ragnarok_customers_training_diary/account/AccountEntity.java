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

    /** ScoutMeto kolo 5: pohlaví. {@code null} = neuvedeno. U žen se nabízí cyklus + cyklus-kalendář. */
    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 8)
    private Gender gender;

    /** ScoutMeto kolo 5: datum narození (pro výpočet věku a maximální tepové frekvence). Nullable. */
    @Column(name = "birth_date")
    private java.time.LocalDate birthDate;

    @Column(name = "email_notifications_enabled", nullable = false)
    private boolean emailNotificationsEnabled = true;

    // -------- Phase 6: email confirmation --------
    /** {@code true} po úspěšném zadání 6místného kódu z emailu. */
    @Column(name = "email_confirmed", nullable = false)
    private boolean emailConfirmed = false;

    /** Aktuální 6místný kód odeslaný na email. {@code null} po potvrzení. */
    @Column(name = "email_confirmation_code", length = 8)
    private String emailConfirmationCode;

    @Column(name = "email_confirmation_code_sent_at")
    private LocalDateTime emailConfirmationCodeSentAt;

    @Column(name = "email_confirmation_code_expires_at")
    private LocalDateTime emailConfirmationCodeExpiresAt;

    // -------- Phase 6: notifikační preference --------
    @Column(name = "notif_group_training_reminder", nullable = false)
    private boolean notifGroupTrainingReminder = true;

    @Column(name = "notif_new_plan_assigned", nullable = false)
    private boolean notifNewPlanAssigned = true;

    @Column(name = "notif_new_comment", nullable = false)
    private boolean notifNewComment = true;

    @Column(name = "notif_welcome", nullable = false)
    private boolean notifWelcome = true;

    /** Phase 19b: potvrzení o vytvoření tréninku (klient si vytvořil trénink). */
    @Column(name = "notif_training_created", nullable = false)
    private boolean notifTrainingCreated = true;

    /** Phase 19b: upozornění, že admin uvedl účet do neaktivního režimu. */
    @Column(name = "notif_account_deactivated", nullable = false)
    private boolean notifAccountDeactivated = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Soft-delete timestamp. {@code null} = aktivní účet. Když je nastaven, účet
     * je anonymizován (email, jméno, telefon přepsány) a v dotazech aktivních
     * účtů se ignoruje.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Phase 10: deaktivace účtu (read-only mód pro neplatiče). {@code null} = aktivní.
     * Když má hodnotu, klient se může přihlásit a prohlížet, ale nesmí přidávat/upravovat
     * tréninky a nevidí nabídky skupinových lekcí. Liší se od {@link #deletedAt} (soft delete).
     */
    @Column(name = "deactivated_at")
    private LocalDateTime deactivatedAt;

    /** Konvenience getter — `deletedAt == null`. */
    public boolean isActive() {
        return deletedAt == null;
    }

    /** Žena? (pro zobrazení cyklu — kontrola na {@code null}). */
    public boolean isFemale() {
        return gender == Gender.FEMALE;
    }

    /**
     * Aktuální věk z {@link #birthDate}. {@code null}, pokud datum narození není vyplněné.
     */
    public Integer getAge() {
        if (birthDate == null) {
            return null;
        }
        return java.time.Period.between(birthDate, java.time.LocalDate.now()).getYears();
    }

    /**
     * Orientační maximální tepová frekvence (ScoutMeto kolo 5). Vzorec dle pohlaví:
     *  - muži: 208 − 0,77 × věk
     *  - ženy: 206 − 0,88 × věk
     * {@code null}, pokud chybí věk nebo pohlaví.
     */
    public Integer getMaxHeartRate() {
        Integer age = getAge();
        if (age == null || gender == null) {
            return null;
        }
        double mtf = (gender == Gender.FEMALE)
                ? 206 - 0.88 * age
                : 208 - 0.77 * age;
        return (int) Math.round(mtf);
    }

    /** {@code true} pokud je účet deaktivovaný (read-only mód). */
    public boolean isDeactivated() {
        return deactivatedAt != null;
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

    /**
     * Účet je enabled, pokud není soft-deleted ANI nepotvrzený emailem.
     * Nepotvrzené účty se nesmí přihlásit — chrání nás před boty.
     * <p>
     * Pozn: pro lepší UX místo "Disabled" hlášky uživateli se v
     * {@link AccountUserDetailsService} a {@code AuthenticationFailureHandler}
     * detekuje rozdíl a uživatel je redirectnut na /confirm-email.
     */
    @Override
    public boolean isEnabled() {
        return deletedAt == null && emailConfirmed;
    }
}
