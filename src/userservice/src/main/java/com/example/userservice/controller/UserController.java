package com.example.userservice.controller;

import com.example.userservice.dto.UserDto;
import com.example.userservice.repository.UserEntity;
import com.example.userservice.service.UserService;
import com.example.userservice.vo.RequestUser;
import com.example.userservice.vo.ResponseUser;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("")
public class UserController {

    UserService userService;
    private RedisTemplate<String,Object> redisTemplate;
    private Environment env;

    @Autowired
    public UserController(UserService userService,
                          RedisTemplate<String,Object> redisTemplate,
                          Environment env){
        this.redisTemplate = redisTemplate;
        this.userService = userService;
        this.env = env;
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

    @PostMapping("/user/refresh")
    public Map<String, Object> requestAccessToken(@RequestBody Map<String, String> m) throws ExpiredJwtException {
        String userId = null;
        Map<String, Object> map = new HashMap<>();
        String expiredAccessToken = m.get("accessToken");
        String refreshToken = m.get("refreshToken");

//        만료된 accessToken에서 email 가져오기 시도
        try{
            userId = Jwts.parser().setSigningKey(env.getProperty("token.secret"))
                    .parseClaimsJws(expiredAccessToken).getBody()
                    .getSubject();
        } catch (ExpiredJwtException e) {
            userId = e.getClaims().getSubject();
        } if (userId == null) throw new IllegalArgumentException(); // 토큰 검증 에러

        String email = userService.getUserDetailsByUserId(userId).getEmail();
        ValueOperations<String, Object> vop = redisTemplate.opsForValue();

        String refreshTokenFromDb = (String) vop.get(email);

//        저장된 refreshToken와 body의 refreshToken이 같은지 확인
        if (!refreshToken.equals(refreshTokenFromDb)){
            map.put("error","refresh token 검증 error");
        }

//        refresh 토큰 만료 확인
        try{
            email = Jwts.parser().setSigningKey(env.getProperty("token.secret"))
                    .parseClaimsJws(refreshToken).getBody()
                    .getSubject();
        } catch (ExpiredJwtException e){
            map.put("error",e);
        }


//          다 통과했다면 accessToken 반환
        String newAccessToken = Jwts.builder()
                .setSubject(userId)
                .setExpiration(new Date(System.currentTimeMillis() +
                        Long.parseLong(env.getProperty("token.access_expiration_time"))))
                .signWith(SignatureAlgorithm.HS512, env.getProperty("token.secret"))
                .compact();

        map.put("accessToken", newAccessToken);
        map.put("refreshToken", refreshToken);

        return map;
    }
}
