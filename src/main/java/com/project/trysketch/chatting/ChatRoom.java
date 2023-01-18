package com.project.trysketch.chatting;

import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;

@Getter
@Setter
public class ChatRoom implements Serializable {

    // redis 에 저장되는 객체들은 Serialize 가능해야함
    private static final long serialVersionUID = 6494678977089006639L;
    private String roomId;
    private String name;

//    public ChatRoom(String roomId, String name) {
//        this.roomId = roomId;
//        this.name = name;
//    }
}

