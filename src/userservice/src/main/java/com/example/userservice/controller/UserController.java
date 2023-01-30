package com.example.userservice.controller;

import com.example.userservice.dto.UserDto;
import com.example.userservice.repository.UserEntity;
import com.example.userservice.service.UserService;
import com.example.userservice.vo.RequestUser;
import com.example.userservice.vo.ResponseUser;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("")
public class UserController {

    UserService userService;

    @Autowired
    public UserController(UserService userService){
        this.userService = userService;
    }

    @PostMapping("/user")
    public ResponseEntity createUser(@RequestBody RequestUser user){
        ModelMapper mapper = new ModelMapper();
//        변환 객체간 필드 명이 정확이 일치해야함
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        UserDto userDto = mapper.map
                (user, UserDto.class);
        userService.createUser(userDto);

        ResponseUser responseUser = mapper.map(userDto, ResponseUser.class);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseUser);
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<ResponseUser> getUser(@PathVariable("id") String userId) {
        UserDto userDto = userService.getUserDetailsByUserId(userId);

        ResponseUser returnValue = new ModelMapper().map(userDto, ResponseUser.class);

        return ResponseEntity.status(HttpStatus.OK).body(returnValue);
    }

    @PostMapping("/user/{id}")
    public ResponseEntity<ResponseUser> updateUser(@PathVariable("id") String userId,
                                                   @RequestBody RequestUser userInfo) {
        UserDto userDto = userService.updateUser(userId, userInfo);

        ResponseUser returnValue = new ModelMapper().map(userDto, ResponseUser.class);

        return ResponseEntity.status(HttpStatus.OK).body(returnValue);
    }

    @DeleteMapping("/user/{id}")
    public ResponseEntity deleteUser(@PathVariable("id") String userId) {
        userService.deleteUser(userId);

        return ResponseEntity.status(HttpStatus.OK).body("사용자 삭제 완료" + userId);
    }

    @GetMapping("/users")
    public ResponseEntity<List<ResponseUser>> getUsers() {
        Iterable<UserEntity> userList = userService.getAllUser();

        List<ResponseUser> result = new ArrayList<>();
        userList.forEach(v -> {
            result.add(new ModelMapper().map(v, ResponseUser.class));
        });

        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @PostMapping("/user/{my_email}/add/{user_email}")
    public ResponseEntity<ResponseUser> addFriend(@PathVariable("user_email") String username,
                                                  @PathVariable("my_email") String myemail) {
        userService.addFriend(myemail,username);

        ResponseUser response = new ModelMapper().map(userService.getUserByEmail(myemail), ResponseUser.class);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/user/{my_email}/add/{user_email}")
    public ResponseEntity<ResponseUser> deleteFriend(@PathVariable("user_email") String username,
                                                  @PathVariable("my_email") String myemail) {
        userService.deleteFriend(myemail,username);

        ResponseUser response = new ModelMapper().map(userService.getUserByEmail(myemail), ResponseUser.class);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
