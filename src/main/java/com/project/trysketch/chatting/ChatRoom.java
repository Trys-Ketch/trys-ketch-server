//package com.project.trysketch.chatting;
//
//import lombok.Getter;
//import lombok.Setter;
//import java.io.Serializable;
//
//// 1. 기능    : 채팅 Room
//// 2. 작성자  : 황미경, 서혁수, 안은솔
//@Getter
//@Setter
//public class ChatRoom implements Serializable {
//    // Redis에 저장되는 객체들은 Serialize 하므로 SerialVersionUID 를 세팅해줌
//    // 모든 Class는 UID를 가지고 있는데 Class의 내용이 변경되면 UID 값 역시 같이 바뀌어 버림.
//    // 직렬화 과정에서 UID로 통신했던 부분이 바뀌어 버리게 되면 다른 Class로 인식을 해버리게 되므로, 이를 방지하기 위해 고유값으로 미리 명시를 해주는 부분
//    private static final long serialVersionUID = 6494678977089006639L;
//    private String roomId;
//    private String name;
//}
//
