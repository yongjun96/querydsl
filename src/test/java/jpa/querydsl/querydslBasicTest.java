package jpa.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryFactory;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnit;
import jpa.querydsl.Dto.MemberDto;
import jpa.querydsl.Dto.QMemberDto;
import jpa.querydsl.Dto.UserDto;
import jpa.querydsl.Entity.Member;
import jpa.querydsl.Entity.QMember;
import jpa.querydsl.Entity.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

import static jpa.querydsl.Entity.QMember.member;
import static jpa.querydsl.Entity.QTeam.team;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@Rollback(false)
public class querydslBasicTest {

    @PersistenceContext
    EntityManager em;

    JPAQueryFactory queryFactory;

    //테스트가 시작되지 전에 실행되는 메소드
    @BeforeEach
    public void befor(){
        queryFactory = new JPAQueryFactory(em);

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
    }

    @Test
    public void startJPQL(){
        //member1을 찾아라
        Member findMember = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "user1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("user1");
    }

    @Test
    public void startQuerydsl(){

        QMember member = new QMember("member1");

        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("user1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("user1");
    }

    @Test
    public void search(){
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("user1")
                        .and(member.age.eq(10))
                )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("user1");
    }

    @Test
    public void searchAndParam(){
        Member findMember = queryFactory
                .selectFrom(member)
                //and는 ,로 자를 수 있음
                .where(member.username.eq("user1"),
                        member.age.between(10, 30)
                )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("user1");
    }

    @Test
    public void resultFetch(){

//        List<Member> fetch = queryFactory
//                .selectFrom(member)
//                .fetch();
//
//        Member fetchOne = queryFactory
//                .selectFrom(member)
//                .fetchOne();
//
//        Member fetchFirst = queryFactory
//                .selectFrom(member)
//                .fetchFirst();

        //복잡한 쿼리에서는 쓰면 안된다.
        QueryResults<Member> fetchResults = queryFactory
                .selectFrom(member)
                .fetchResults();

        fetchResults.getTotal();
        List<Member> content = fetchResults.getResults();

        //select절을 다 지우고 카운트를 가져온다.
        long count = queryFactory
                .selectFrom(member)
                .fetchCount();
    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(asc)
     * 2. 회원 아름 오름차순(desc)
     * 단 2에서 회원 이름이 없으면 마지막에 출력 (nulls last)
     */
    @Test
    public void sort(){

        em.persist(new Member(null, 100));
        em.persist(new Member("user5", 100));
        em.persist(new Member("user6", 100));

        List<Member> fetch = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = fetch.get(0);
        Member member6 = fetch.get(1);
        Member memberNull = fetch.get(2);

        assertThat(member5.getUsername()).isEqualTo("user5");
        assertThat(member6.getUsername()).isEqualTo("user6");
        assertThat(memberNull.getUsername()).isNull();

    }

    @Test
    public void paging1(){
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    public void paging2(){
        QueryResults<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        assertThat(result.getTotal()).isEqualTo(4);
        assertThat(result.getLimit()).isEqualTo(2);
        assertThat(result.getOffset()).isEqualTo(1);
        assertThat(result.getResults().size()).isEqualTo(2);
    }

    @Test
    public void aggregation() {

        //쿼리 DSL이 제공하는 튜플 (실무에서는 Dto로 꺼내오고 튜플을 잘 쓰지는 않음)
        List<Tuple> fetch = queryFactory
                .select(member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = fetch.get(0);

        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
    }

    /**
     * 팀의 이름과 각 팀의 평균 연령을 구해라
     * @throws Exception
     */
    @Test
    public void group() throws Exception{

        List<Tuple> fetch = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                //.having(member.age.gt(10))
                .fetch();

        Tuple teamA = fetch.get(0);
        Tuple teamB = fetch.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    /**
     * teamA에 소속된 모른 회원을 찾아라
     */
    @Test
    public void join(){

        List<Member> result = queryFactory
                .selectFrom(member)
                .rightJoin(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("user1", "user2");
    }

    /**
     * 세타 조인
     * 회원의 이름이 팀이름과 같은 회원을 조회
     */
    @Test
    public void theta_join(){

        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        //모든 회원과 모든 팀을 다 가져와서 조인을 해버리고 조건절에 조건으로 필터링
        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /**
     * ex). 회원과 팀을 조인하면서 팀이름이 teamA인 탐만 조인, 회원은 모두 조회
     */
    @Test
    public void join_on(){

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .join(member.team, team).on(team.name.eq("teamA"))
                .fetch();

        result.forEach(m -> System.out.println(m.toString()));

    }


    /**
     * 연관관계가 없는 엔티티 외부 조인
     * 회원의 이름이 팀 이름과 같은 대상 외부 조인
     */
    @Test
    public void join_on_no_relation(){

        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        //모든 회원과 모든 팀을 다 가져와서 조인을 해버리고 조건절에 조건으로 필터링
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                //원래는 member.team으로 id를 조인하지만 이렇게 하면 id를 무시하고 name과 username이 같은 경우만 조회
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();

        result.forEach(m -> System.out.println(m.toString()));
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo(){
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("user1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 미적용").isFalse();
    }

    @Test
    public void fetchJoinUse(){
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("user1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 적용").isTrue();
    }

    /**
     * 나이가 가장 많은 회원을 조회
     */
    @Test
    public void subQuery(){

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub)
                        )
                )
                .fetch();

        assertThat(result).extracting("age").containsExactly(40);
    }

    /**
     * 나이가 평균 이상인 회원
     */
    @Test
    public void subQueryGoe(){

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                        )
                )
                .fetch();

        assertThat(result).extracting("age").containsExactly(30, 40);
    }

    /**
     * 나이가 10세 이상인 회원
     */
    @Test
    public void subQueryIn(){

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(JPAExpressions
                                .select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                        )
                )
                .fetch();

        assertThat(result).extracting("age").containsExactly(20, 30, 40);
    }

    @Test
    public void selectSubQuery(){

        QMember memberSub = new QMember("memberSub");

        List<Tuple> fetch = queryFactory
                .select(member.username,
                        JPAExpressions.select(memberSub)
                                .select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();

        fetch.forEach(m -> System.out.println(m.toString()));
    }

    @Test
    public void basicCase(){

        List<String> fetch = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        fetch.forEach(m -> System.out.println(m.toString()));
    }

    @Test
    public void complexCase(){

        List<String> fetch = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        fetch.forEach(m -> System.out.println(m.toString()));
    }

    @Test
    public void constant(){
        List<Tuple> fetch = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        fetch.forEach(m -> System.out.println(m.toString()));
    }

    @Test
    public void concat(){

        //{username}_{age}
        List<String> fetch = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("user1"))
                .fetch();

        fetch.forEach(m -> System.out.println(m.toString()));
    }

    @Test
    public void simpleProjection(){

        List<String> fetch = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        fetch.forEach(m -> System.out.println(m.toString()));
    }

    @Test
    public void tupleProjection(){
        List<Tuple> fetch = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        fetch.forEach(m -> {
            System.out.println(m.get(member.username));
            System.out.println(m.get(member.age));
        });
    }

    @Test
    public void findDtoByJPQL(){
        //뉴오퍼레이션 방식
        List<MemberDto> members =
                em.createQuery("select new jpa.querydsl.Dto.MemberDto(m.username, m.age) " +
                "from Member m", MemberDto.class).getResultList();

        members.forEach(m -> System.out.println(m.toString()));
    }

    @Test
    public void findByDtoSetter(){
        List<MemberDto> members = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        members.forEach(m -> System.out.println(m.toString()));
    }

    @Test
    public void findByDtoField(){
        List<MemberDto> members = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        members.forEach(m -> System.out.println(m.toString()));
    }

    @Test
    public void findByDtoConstructor(){
        List<MemberDto> members = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        members.forEach(m -> System.out.println(m.toString()));

    }

    @Test
    public void findUserDto(){

        QMember memberSub = new QMember("memberSub");

        List<UserDto> members = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),
                        ExpressionUtils.as(JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub), "age")
                ))
                .from(member)
                .fetch();

        members.forEach(m -> System.out.println(m.toString()));
    }

    @Test
    public void findDtoVyQueryProjection(){
        List<MemberDto> fetch = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        fetch.forEach(m -> System.out.println(m.toString()));
    }

    @Test
    public void dynamicQueryBooleanBuilder(){
        String usernameParam = "user1";
        Integer ageParam = null;

        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {

        BooleanBuilder builder = new BooleanBuilder();

        if(usernameCond != null){
            builder.and(member.username.eq(usernameCond));
        }

        if(ageCond != null){
            builder.and(member.age.eq(ageCond));
        }

        return queryFactory
                .select(member)
                .from(member)
                .where(builder)
                .fetch();
    }

    @Test
    public void dynamicQueryWhereParam(){
        String usernameParam = "user1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {

        return queryFactory
                .select(member)
                .from(member)
                //.where(allEq(usernameCond, ageCond))
                .where(usernameEq(usernameCond), ageEq(ageCond))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        if(usernameCond == null){
            return null;
        }else {
            return member.username.eq(usernameCond);
        }
    }

    private BooleanExpression ageEq(Integer ageCond) {
        if(ageCond == null){
            return null;
        }else {
            return member.age.eq(ageCond);
        }
    }

    //광고 상태 isValid, 날짜가 IN : isServiceable
    private BooleanExpression allEq(String usernameCond, Integer ageCond){
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    @Test
    public void bulkUpdate(){

        //영속성 user1 = 10 -> DB 비회원
        //영속성 user2 = 20 -> DB 비회원
        //영속성 user3 = 30 -> DB user3
        //영속성 user4 = 40 -> DB user4

        //벌크 연산은 영속성 컨텍스트를 무시하고 DB에 바로 쿼리가 나가기 때문에
        // 영속성 컨텍스트는 "비회원"으로 업데이트 되지 않고 user~로 유지된다.
        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        //리피티블 리드 현상 해결
        em.flush();
        em.clear();

        List<Member> fetch = queryFactory
                .select(member)
                .from(member)
                .fetch();

        fetch.forEach(m -> System.out.println(m.toString()));

        assertThat(count).isEqualTo(2);
    }


    @Test
    public void bulkAdd(){

        queryFactory
                .update(member)
                .set(member.age, member.age.multiply(2))
                .execute();
    }

    @Test
    public void bulkDelete(){
        queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
    }

    @Test
    public void sqlFunction(){

        List<String> fetch = queryFactory
                .select(
                        Expressions.stringTemplate("function('replace', {0}, {1}, {2})",
                                member.username, "user", "M")
                )
                .from(member)
                .fetch();

        fetch.forEach(m -> System.out.println(m.toString()));
    }

    @Test
    public void sqlFunction2(){
        List<String> fetch = queryFactory
                .select(member.username)
                .from(member)
                .where(
                        member.username.eq(
                                //Expressions.stringTemplate("function('lower', {0})", member.username)
                                member.username.lower()
                        )
                )
                .fetch();

        fetch.forEach(m -> System.out.println(m.toString()));
    }
}
