package jpa.querydsl.repository;

import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jpa.querydsl.Dto.MemberSearchCondition;
import jpa.querydsl.Dto.MemberTeamDto;
import jpa.querydsl.Dto.QMemberTeamDto;
import jpa.querydsl.Entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;

import static jpa.querydsl.Entity.QMember.member;
import static jpa.querydsl.Entity.QTeam.team;
import static org.springframework.util.StringUtils.hasText;

@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberRepositoryCustom{

//      QuerydslRepositorySupport
//    extends QuerydslRepositorySupport
//    public MemberRepositoryImpl() {
//        super(Member.class);
//    }

    private final JPAQueryFactory queryFactory;

    @Override
    public List<MemberTeamDto> search (MemberSearchCondition condition){

            return queryFactory
                    .select(new QMemberTeamDto(
                            member.id.as("memberId"),
                            member.username,
                            member.age,
                            team.id.as("teamId"),
                            team.name.as("teamName"))
                    )
                    .from(member)
                    .join(member.team, team)
                    .where(
                            userNameEq(condition.getUsername()),
                            teamNameEq(condition.getTeamName()),
                            ageGoe(condition.getAgeGoe()),
                            ageLoe(condition.getAgeLoe())
                    )
                    .fetch();
        }

    @Override
    public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable) {

        QueryResults<MemberTeamDto> results =
                queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName"))
                )
                .from(member)
                .join(member.team, team)
                .where(
                        userNameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .offset(pageable.getOffset()) // 몇번째 페이지 부터~
                .limit(pageable.getPageSize()) // 페이지당 몇개까지 ~
                .fetchResults(); // 컨텐츠용 쿼리, 카운터 쿼리 두번 날림.

        List<MemberTeamDto> content = results.getResults();
        long total = results.getTotal();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable) {

        List<MemberTeamDto> content=
                queryFactory
                        .select(new QMemberTeamDto(
                                member.id.as("memberId"),
                                member.username,
                                member.age,
                                team.id.as("teamId"),
                                team.name.as("teamName"))
                        )
                        .from(member)
                        .join(member.team, team)
                        .where(
                                userNameEq(condition.getUsername()),
                                teamNameEq(condition.getTeamName()),
                                ageGoe(condition.getAgeGoe()),
                                ageLoe(condition.getAgeLoe())
                        )
                        .offset(pageable.getOffset()) // 몇번째 페이지 부터~
                        .limit(pageable.getPageSize()) // 페이지당 몇개까지 ~
                        .fetch();

        JPAQuery<Member> count = queryFactory
                .select(member)
                .from(member)
                .join(member.team, team)
                .where(
                        userNameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                );

        // 페이지의 사이즈가 작거나, 마지막페이지의 사이즈가 0이거나 하면 카운트쿼리를 호출 하지않음.
        // 성능개선!!!
        return PageableExecutionUtils.getPage(content, pageable, () -> count.fetchCount());
        //같은 표기법
        //return PageableExecutionUtils.getPage(content, pageable, count::fetchCount);

        //return new PageImpl<>(content, pageable, total);
    }

        private BooleanExpression userNameEq(String username) {
            return hasText(username) ? member.username.eq(username) : null;
        }

        private BooleanExpression teamNameEq(String teamName) {
            return hasText(teamName) ? team.name.eq(teamName) : null;
        }

        private BooleanExpression ageGoe(Integer ageGoe) {
            return ageGoe != null ? member.age.goe(ageGoe) : null;
        }

        private BooleanExpression ageLoe(Integer ageLoe) {
            return ageLoe != null ? member.age.loe(ageLoe) : null;
        }
}
