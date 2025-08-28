1. 알림 설정

locationShare 패키지에 FCM 서비스가 있다.
fcm service를 참고하고
content 패키지에서 댓글, 좋아요가 게시글에 달리면 해당 게시글의 작성자를 대상으로

- 댓글
```
data: null
isRead: false
message: ${누른사람: 익명이면 익명} 님이 ${해당 게시글의 제목 최대 앞에서 8글자} 글에 댓글을 작성했습니다. 
title: 댓글 알림
type: normal
```

- 좋아요
```
data: null
isRead: false
message: ${누른사람: 익명이면 익명} 님이 ${해당 게시글의 제목 최대 앞에서 8글자} 글을 좋아합니다. 
title: 좋아요 알림
type: normal
```

알림을 저장과 동시에

내부적으로 API를 보내 fcm 서비스를 참고하여 알림을 발송하는 API를 요청하도록 한다.

알림 요청에 관한 내용은 notification과 locationShare 패키지를 탐색해 찾아라