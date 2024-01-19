package jpa.querydsl.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jpa.querydsl.Dto.MemberSearchCondition;
import jpa.querydsl.Dto.MemberTeamDto;
import jpa.querydsl.Entity.Member;
import jpa.querydsl.Entity.Team;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {

    @Autowired EntityManager em;
    @Autowired MemberJpaRepository memberJpaRepository;

    @Test
    public void basicTest(){
        Member member = new Member("member1", 10);
        memberJpaRepository.save(member);

        Optional<Member> findMember = memberJpaRepository.findById(member.getId());

        assertThat(findMember.get().getUsername()).isEqualTo("member1");
        assertThat(member).isEqualTo(findMember.get());

        List<Member> findMembers = memberJpaRepository.findAllQuerydsl();
        assertThat(findMembers).containsExactly(member);

        List<Member> findMemberUsername = memberJpaRepository.findByUsernameQuerydsl("member1");

        assertThat(findMemberUsername).containsExactly(member);
    }

    @Test
    public void searchTest(){

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("user1", 10, teamA);
        Member member2 = new Member("user2", 20, teamA);

        Member member3 = new Member("user3", 30, teamB);
        Member member4 = new Member("user4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        MemberSearchCondition condition = new MemberSearchCondition();
        //condition.setAgeGoe(35);
        //condition.setAgeLoe(40);
        condition.setTeamName("teamB");

        List<MemberTeamDto> resultBuilder = memberJpaRepository.searchByBuilder(condition);
        List<MemberTeamDto> resultWhere = memberJpaRepository.search(condition);

        assertThat(resultBuilder).extracting("username").containsExactly("user3", "user4");
        assertThat(resultWhere).extracting("username").containsExactly("user3", "user4");

    }



}