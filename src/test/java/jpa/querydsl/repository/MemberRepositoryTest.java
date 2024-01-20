package jpa.querydsl.repository;

import jakarta.persistence.EntityManager;
import jpa.querydsl.Dto.MemberSearchCondition;
import jpa.querydsl.Dto.MemberTeamDto;
import jpa.querydsl.Entity.Member;
import jpa.querydsl.Entity.Team;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberRepositoryTest {

    @Autowired EntityManager em;

    @Autowired MemberRepository memberRepository;

    @Test
    public void basicTest(){
        Member member = new Member("member1", 10);
        memberRepository.save(member);

        Optional<Member> findMember = memberRepository.findById(member.getId());

        assertThat(findMember.get().getUsername()).isEqualTo("member1");
        assertThat(member).isEqualTo(findMember.get());

        List<Member> findMembers = memberRepository.findAll();
        assertThat(findMembers).containsExactly(member);

        List<Member> findMemberUsername = memberRepository.findByUsername("member1");

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

        List<MemberTeamDto> resultWhere = memberRepository.search(condition);

        assertThat(resultWhere).extracting("username").containsExactly("user3", "user4");

    }


    @Test
    public void searchPageSimple(){

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
        //condition.setTeamName("teamB");

        PageRequest pageRequest = PageRequest.of(0, 3);

        Page<MemberTeamDto> resultPage = memberRepository.searchPageSimple(condition, pageRequest);

        assertThat(resultPage.getSize()).isEqualTo(3);
        assertThat(resultPage.getContent()).extracting("username").containsExactly("user1", "user2", "user3");

    }

}