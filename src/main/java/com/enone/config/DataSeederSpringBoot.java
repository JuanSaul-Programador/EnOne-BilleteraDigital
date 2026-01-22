package com.enone.config;


import com.enone.domain.model.Role;
import com.enone.domain.model.User;
import com.enone.domain.model.UserProfile;
import com.enone.domain.repository.RoleRepository;
import com.enone.domain.repository.UserProfileRepository;
import com.enone.domain.repository.UserRepository;
import com.enone.util.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Component
public class DataSeederSpringBoot implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Override
    public void run(String... args) throws Exception {
        createRolesIfNotExist();
        createAdminUserIfNotExist();
    }

    @Transactional
    private void createRolesIfNotExist() {
        if (roleRepository.count() > 0) {
            System.out.println("Roles ya existen, omitiendo creación");
            return;
        }


        Role userRole = new Role();
        userRole.setName("ROLE_USER");
        roleRepository.save(userRole);
        System.out.println("Rol ROLE_USER creado");


        Role clientRole = new Role();
        clientRole.setName("CLIENT");
        roleRepository.save(clientRole);
        System.out.println("Rol CLIENT creado");


        Role adminRole = new Role();
        adminRole.setName("ADMIN");
        roleRepository.save(adminRole);
        System.out.println("Rol ADMIN creado");
    }

    @Transactional
    private void createAdminUserIfNotExist() {
        Optional<User> existingAdmin = userRepository.findByUsername("admin@enone.com");
        
        if (existingAdmin.isPresent()) {
            ensureAdminHasAdminRole(existingAdmin.get());
            return;
        }

        try {
            Role roleUser = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Rol ROLE_USER no encontrado"));
            Role roleAdmin = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new RuntimeException("Rol ADMIN no encontrado"));

            User adminUser = new User();
            adminUser.setUsername("admin@enone.com");
            adminUser.setPassword(PasswordUtil.hash("admin123"));
            adminUser.setEnabled(true);
            adminUser.getRoles().add(roleUser);
            adminUser.getRoles().add(roleAdmin);
            
            adminUser = userRepository.save(adminUser);

            UserProfile profile = new UserProfile();
            profile.setUser(adminUser);

            profile.setEmail("admin@enone.com");
            profile.setPhone("+51999999999");
            profile.setFirstName("Admin");
            profile.setLastName("Sistema");
            profile.setDocumentType("DNI");
            profile.setDocumentNumber("00000000");
            profile.setGender("Otro");
            profile.setBirthDate(LocalDate.of(2000, 1, 1));
            profile.setTwoFactorEnabled(false);
            
            userProfileRepository.save(profile);

        } catch (Exception e) {
            throw new RuntimeException("No se pudo crear el usuario administrador", e);
        }
    }

    private void ensureAdminHasAdminRole(User adminUser) {
        boolean hasAdminRole = adminUser.getRoles().stream()
            .anyMatch(role -> "ADMIN".equals(role.getName()));
        
        if (!hasAdminRole) {

            Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new RuntimeException("Rol ADMIN no encontrado"));
            
            adminUser.getRoles().add(adminRole);
            userRepository.save(adminUser);

        }
    }
}