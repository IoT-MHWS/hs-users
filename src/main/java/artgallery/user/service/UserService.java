package artgallery.user.service;

import artgallery.user.configuration.ServerUserDetails;
import artgallery.user.dto.*;
import artgallery.user.entity.RoleEntity;
import artgallery.user.entity.UserEntity;
import artgallery.user.exception.RoleAlreadyExistsException;
import artgallery.user.exception.RoleDoesNotExistException;
import artgallery.user.exception.UserAlreadyExistsException;
import artgallery.user.exception.UserDoesNotExistException;
import artgallery.user.repository.RoleRepository;
import artgallery.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
  @Autowired
  private KafkaTemplate<String, EmailDTO> kafkaTemplate;

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;

  public Mono<UserCreatedDTO> create(UserDTO userDTO) {
    return Mono.just(userDTO)
      .subscribeOn(Schedulers.boundedElastic())
      .<UserEntity>handle((dto, sink) -> {
        if (userRepository.existsByLogin(dto.getLogin())) {
          sink.error(new UserAlreadyExistsException(dto.getLogin()));
          return;
        }

        var userEntity = UserEntity.builder()
          .login(dto.getLogin())
          .password(passwordEncoder.encode(dto.getPassword()))
          .email(dto.getEmail())
          .roles(new ArrayList<>())
          .build();

        var roleEntity = roleRepository.findByName(Role.PUBLIC.name());

        if (roleEntity.isEmpty()) {
          sink.error(new RoleDoesNotExistException(Role.PUBLIC));
          return;
        }

        userEntity.getRoles().add(roleEntity.get());
        userRepository.save(userEntity);
        sink.next(userEntity);

        EmailDTO emailDTO = new EmailDTO();
        emailDTO.setEmail(dto.getEmail());
        kafkaTemplate.send("email", emailDTO);
      }).map(entity -> new UserCreatedDTO(entity.getId(), entity.getLogin(), entity.getEmail()));
  }

  public Mono<UserDetailsDTO> getUserDetails(ServerUserDetails userDetails) {
    return Mono.just(new UserDetailsDTO(
      userDetails.getId(),
      userDetails.getUsername(),
      userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()))
    );
  }

  public Mono<Void> addRole(String login, Role role) {
    return Mono.just(login)
      .subscribeOn(Schedulers.boundedElastic())
      .<UserEntity>handle((val, sink) -> {
        try {
          UserEntity entity = addRoleBlocking(login, role);
          sink.next(entity);
        } catch (Exception ex) {
          sink.error(ex);
        }
      }).then();
  }

  @Transactional
  private UserEntity addRoleBlocking(String login, Role role) {
    var userEntityOpt = userRepository.findByLogin(login);
    if (userEntityOpt.isEmpty()) {
      throw new UserDoesNotExistException(login);
    }
    var roleEntityOpt = roleRepository.findByName(role.name());
    if (roleEntityOpt.isEmpty()) {
      throw new RoleDoesNotExistException(role);
    }

    var userEntity = userEntityOpt.get();
    var roleEntity = roleEntityOpt.get();

    if (userEntity.getRoles().contains(roleEntity)) {
      throw new RoleAlreadyExistsException(role);
    }

    userEntity.getRoles().add(roleEntity);
    roleRepository.save(roleEntity);
    return userRepository.save(userEntity);
  }

  public Mono<Void> removeRole(String login, Role role) {
    return Mono.just(login)
      .subscribeOn(Schedulers.boundedElastic())
      .<UserEntity>handle((val, sink) -> {
        try {
          UserEntity entity = removeRoleBlocking(login, role);
          sink.next(entity);
        } catch (Exception ex) {
          sink.error(ex);
        }
      }).then();
  }

  @Transactional
  private UserEntity removeRoleBlocking(String login, Role role) {
    var userEntityOpt = userRepository.findByLogin(login);
    if (userEntityOpt.isEmpty()) {
      throw new UserDoesNotExistException(login);
    }
    var roleEntityOpt = roleRepository.findByName(role.name());
    if (roleEntityOpt.isEmpty()) {
      throw new RoleDoesNotExistException(role);
    }

    var userEntity = userEntityOpt.get();
    var roleEntity = roleEntityOpt.get();

    if (!userEntity.getRoles().contains(roleEntity)) {
      throw new RoleDoesNotExistException(role);
    }

    userEntity.getRoles().remove(roleEntity);
    return userRepository.save(userEntity);
  }

  public Flux<RoleDTO> getRoles(String login) {
    return Mono.just(login)
      .subscribeOn(Schedulers.boundedElastic())
      .<List<RoleEntity>>handle((val, sink) -> {
        var userEntityOpt = userRepository.findByLogin(login);
        if (userEntityOpt.isEmpty()) {
          sink.error(new UserDoesNotExistException(login));
          return;
        }
        sink.next(userEntityOpt.get().getRoles());
      })
      .flatMapMany(Flux::fromIterable)
      .map(entity -> new RoleDTO(Role.valueOf(entity.getName())));
  }
}
