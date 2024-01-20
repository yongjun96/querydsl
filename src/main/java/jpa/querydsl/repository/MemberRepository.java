package jpa.querydsl.repository;

import jpa.querydsl.Entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {
    //select m from Mebmer m where m.username = :username;
    List<Member> findByUsername(String username);
}
