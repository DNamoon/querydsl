package study.querydsl.repository;

import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;

import java.util.List;

public class MemberRepositoryImpl implements MemberRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public MemberRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public List<MemberTeamDto> search(MemberSearchCondition condition) {
        return queryFactory
            .select(new QMemberTeamDto(
                QMember.member.id.as("memberId"),
                QMember.member.username,
                QMember.member.age,
                QTeam.team.id.as("teamId"),
                QTeam.team.name.as("teamName")))
            .from(QMember.member)
            .leftJoin(QMember.member.team, QTeam.team)
            .where(usernameEq(condition.getUsername()),
                teamNameEq(condition.getTeamName()),
                ageGoe(condition.getAgeGoe()),
                ageLoe(condition.getAgeLoe()))
            .fetch();
    }

    @Override
    public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable) {
        QueryResults<MemberTeamDto> results = queryFactory
            .select(new QMemberTeamDto(
                QMember.member.id.as("memberId"),
                QMember.member.username,
                QMember.member.age,
                QTeam.team.id.as("teamId"),
                QTeam.team.name.as("teamName")))
            .from(QMember.member)
            .leftJoin(QMember.member.team, QTeam.team)
            .where(usernameEq(condition.getUsername()),
                teamNameEq(condition.getTeamName()),
                ageGoe(condition.getAgeGoe()),
                ageLoe(condition.getAgeLoe()))
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetchResults();

        //.fetchResults 사용 자제 권고 (복잡한 쿼리에서 의도하지 않게 동작할 위험 있음)
        //자동으로 만들어지는 count 쿼리가 복잡한 join이나 group by 환경에서 예상과 다르게 동작할 수 있어서
        //→ "직접 count 쿼리를 작성하는 방식으로 바꿔라" 권고됨.
//        // 1️⃣ content 쿼리
//        List<MemberTeamDto> content = queryFactory
//            .select(new QMemberTeamDto(
//                QMember.member.id.as("memberId"),
//                QMember.member.username,
//                QMember.member.age,
//                QTeam.team.id.as("teamId"),
//                QTeam.team.name.as("teamName")))
//            .from(QMember.member)
//            .leftJoin(QMember.member.team, QTeam.team)
//            .where(usernameEq(condition.getUsername()),
//                teamNameEq(condition.getTeamName()),
//                ageGoe(condition.getAgeGoe()),
//                ageLoe(condition.getAgeLoe()))
//            .offset(pageable.getOffset())
//            .limit(pageable.getPageSize())
//            .fetch();
//
//// 2️⃣ total count 쿼리
//        long total = queryFactory
//            .select(QMember.member.count())
//            .from(QMember.member)
//            .leftJoin(QMember.member.team, QTeam.team)
//            .where(usernameEq(condition.getUsername()),
//                teamNameEq(condition.getTeamName()),
//                ageGoe(condition.getAgeGoe()),
//                ageLoe(condition.getAgeLoe()))
//            .fetchOne();
//
//// 3️⃣ Page 객체 반환
//        return new PageImpl<>(content, pageable, total);

        List<MemberTeamDto> content = results.getResults();
        long total = results.getTotal();

        return new PageImpl<>(content, pageable, total);

    }

    @Override
    public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable) {
        List<MemberTeamDto> content = queryFactory
            .select(new QMemberTeamDto(
                QMember.member.id.as("memberId"),
                QMember.member.username,
                QMember.member.age,
                QTeam.team.id.as("teamId"),
                QTeam.team.name.as("teamName")))
            .from(QMember.member)
            .leftJoin(QMember.member.team, QTeam.team)
            .where(usernameEq(condition.getUsername()),
                teamNameEq(condition.getTeamName()),
                ageGoe(condition.getAgeGoe()),
                ageLoe(condition.getAgeLoe()))
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        JPAQuery<Member> countQuery= queryFactory
            .selectFrom(QMember.member)
            .leftJoin(QMember.member.team, QTeam.team)
            .where(usernameEq(condition.getUsername()),
                teamNameEq(condition.getTeamName()),
                ageGoe(condition.getAgeGoe()),
                ageLoe(condition.getAgeLoe()));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchCount);
    }

    private BooleanExpression usernameEq(String username) {
        return StringUtils.hasText(username) ? QMember.member.username.eq(username) : null;
    }

    private BooleanExpression teamNameEq(String teamName) {
        return StringUtils.hasText(teamName) ? QTeam.team.name.eq(teamName) : null;
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe != null ? QMember.member.age.goe(ageGoe) : null;
    }

    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe != null ? QMember.member.age.loe(ageLoe) : null;
    }
}
