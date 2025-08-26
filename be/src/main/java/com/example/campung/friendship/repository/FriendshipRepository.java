package com.example.campung.friendship.repository;

import com.example.campung.entity.Friendship;
import com.example.campung.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    /**
     * 요청자와 수신자 간의 친구 관계 조회 (User 엔티티 기반)
     */
    Optional<Friendship> findByRequesterAndAddressee(User requester, User addressee);

    /**
     * 수신자와 상태로 친구 요청 조회 (User 엔티티 기반)
     */
    List<Friendship> findByAddresseeAndStatus(User addressee, String status);

    /**
     * 요청자와 상태로 친구 요청 조회 (User 엔티티 기반)
     */
    List<Friendship> findByRequesterAndStatus(User requester, String status);

    /**
     * 사용자의 승인된 친구 관계 조회 (요청자이거나 수신자인 경우 모두)
     */
    @Query("SELECT f FROM Friendship f WHERE (f.requester = :user OR f.addressee = :user) AND f.status = 'accepted'")
    List<Friendship> findAcceptedFriendshipsByUser(@Param("user") User user);

    /**
     * 두 사용자 간의 승인된 친구 관계 조회
     */
    @Query("SELECT f FROM Friendship f WHERE ((f.requester = :user1 AND f.addressee = :user2) OR (f.requester = :user2 AND f.addressee = :user1)) AND f.status = 'accepted'")
    Optional<Friendship> findAcceptedFriendshipBetweenUsers(@Param("user1") User user1, @Param("user2") User user2);
}