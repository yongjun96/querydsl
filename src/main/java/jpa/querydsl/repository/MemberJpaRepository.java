package jpa.querydsl.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jpa.querydsl.Dto.MemberSearchCondition;
import jpa.querydsl.Dto.MemberTeamDto;
import jpa.querydsl.Dto.QMemberTeamDto;
import jpa.querydsl.Entity.Member;
import jpa.querydsl.Entity.QMember;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

import static jpa.querydsl.Entity.QMember.*;
import static jpa.querydsl.Entity.QTeam.team;

@Repository
@RequiredArgsConstructor
public class MemberJpaRepository {

    private final EntityManager em;
    private final JPAQueryFactory queryFactory;


    public void save(Member member){
        em.persist(member);
    }

    public Optional<Member>findById(Long id){
        Member findMember = em.find(Member.class, id);
        return Optional.ofNullable(findMember);
    }

    public List<Member> findAll(){
        return em.createQuery("select m from Member m", Member.class).getResultList();
    }

    public List<Member> findAllQuerydsl(){
        return queryFactory.selectFrom(member).fetch();
    }

    public List<Member> findByUsername(String username){
        return em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", username)
                .getResultList();
    }

    public List<Member> findByUsernameQuerydsl(String username){
        return queryFactory.selectFrom(member).where(member.username.eq(username)).fetch();
    }

    public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition){

        BooleanBuilder builder = new BooleanBuilder();

        if(StringUtils.hasText(condition.getUsername())){

        }

        return queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberID"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")))
                .from(member)
                .join(member.team, team).fetchJoin()
                .where(UsernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .fetch();
    }

    public BooleanExpression UsernameEq(String username){
        if(username == null){
            return null;
        }else {
            return member.username.eq(username);
        }
    }

    public BooleanExpression teamNameEq(String teamName){
        if(teamName == null){
            return null;
        }else {
            return member.team.name.eq(teamName);
        }
    }

    public BooleanExpression ageGoe(Integer ageGoe){
        if(ageGoe == null){
            return null;
        }else {
            return member.age.goe(ageGoe);
        }
    }

    public BooleanExpression ageLoe(Integer ageLoe){
        if(ageLoe == null){
            return null;
        }else {
            return member.age.loe(ageLoe);
        }
    }

}
