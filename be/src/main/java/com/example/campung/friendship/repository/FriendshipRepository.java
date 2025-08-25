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
     * 요청자와 수신자 간의 친구 관계 조회 (ID 기반)
     */
    @Query("SELECT f FROM Friendship f WHERE f.requester.id = :requesterId AND f.addressee.id = :addresseeId")
    Optional<Friendship> findByRequesterIdAndAddresseeId(@Param("requesterId") Long requesterId, @Param("addresseeId") Long addresseeId);

    /**
     * 수신자와 상태로 친구 요청 조회
     */
    @Query("SELECT f FROM Friendship f WHERE f.addressee.id = :addresseeId AND f.status = :status")
    List<Friendship> findByAddresseeIdAndStatus(@Param("addresseeId") Long addresseeId, @Param("status") String status);

    /**
     * 요청자와 상태로 친구 요청 조회
     */
    @Query("SELECT f FROM Friendship f WHERE f.requester.id = :requesterId AND f.status = :status")
    List<Friendship> findByRequesterIdAndStatus(@Param("requesterId") Long requesterId, @Param("status") String status);

    /**
     * 사용자의 승인된 친구 관계 조회 (요청자이거나 수신자인 경우 모두)
     */
    @Query("SELECT f FROM Friendship f WHERE (f.requester.id = :userId OR f.addressee.id = :userId) AND f.status = 'accepted'")
    List<Friendship> findAcceptedFriendshipsByUserId(@Param("userId") Long userId);

    /**
     * 두 사용자 간의 승인된 친구 관계 조회
     */
    @Query("SELECT f FROM Friendship f WHERE ((f.requester.id = :userId1 AND f.addressee.id = :userId2) OR (f.requester.id = :userId2 AND f.addressee.id = :userId1)) AND f.status = 'accepted'")
    Optional<Friendship> findAcceptedFriendshipBetweenUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
}