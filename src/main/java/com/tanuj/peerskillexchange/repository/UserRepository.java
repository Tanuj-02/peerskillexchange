//package com.tanuj.peerskillexchange.repository;
//
//import com.tanuj.peerskillexchange.enums.SkillType;
//import com.tanuj.peerskillexchange.model.User;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.EntityGraph;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//
//import java.util.Collection;
//import java.util.List;
//import java.util.Optional;
//
//public interface UserRepository extends JpaRepository<User, String> {
//    Optional<User> findByEmailOrUsername(String email, String username);
//
//    Optional<User> findByEmail(String email);
//
//    Optional<User> findByUsername(String username);
//
//    @EntityGraph(attributePaths = {"userSkills", "userSkills.skill"})
//    Optional<User> findWithSkillsByUsername(String username);
//
//    @EntityGraph(attributePaths = {"userSkills", "userSkills.skill"})
//    @Query("select u from User u where u.id in :ids")
//    List<User> findAllWithSkillsByIdIn(@Param("ids") Collection<String> ids);
//
//    @Query(
//            value = """
//                    select distinct u.id
//                    from User u
//                    join u.userSkills us
//                    join us.skill s
//                    where u.username <> :username
//                      and us.type = :type
//                      and lower(s.skillname) in :skills
//                    order by u.createdAt desc
//                    """,
//            countQuery = """
//                    select count(distinct u)
//                    from User u
//                    join u.userSkills us
//                    join us.skill s
//                    where u.username <> :username
//                      and us.type = :type
//                      and lower(s.skillname) in :skills
//                    """
//    )
//    Page<String> findDiscoverCandidateIds(
//            @Param("username") String username,
//            @Param("type") SkillType type,
//            @Param("skills") List<String> skills,
//            Pageable pageable
//    );
//
//    @Query(
//            value = """
//                    select distinct u.id
//                    from User u
//                    left join u.userSkills us
//                    left join us.skill s
//                    where u.username <> :username
//                      and (
//                            (us.type = :type and lower(s.skillname) in :skills)
//                         or lower(coalesce(u.college, '')) = :college
//                      )
//                    order by
//                      case when lower(coalesce(u.college, '')) = :college then 0 else 1 end,
//                      u.createdAt desc
//                    """,
//            countQuery = """
//                    select count(distinct u)
//                    from User u
//                    left join u.userSkills us
//                    left join us.skill s
//                    where u.username <> :username
//                      and (
//                            (us.type = :type and lower(s.skillname) in :skills)
//                         or lower(coalesce(u.college, '')) = :college
//                      )
//                    """
//    )
//    Page<String> findDiscoverBySkillsOrCollegeIds(
//            @Param("username") String username,
//            @Param("type") SkillType type,
//            @Param("skills") List<String> skills,
//            @Param("college") String college,
//            Pageable pageable
//    );
//
//    @Query(
//            value = """
//                    select u.id
//                    from User u
//                    where u.username <> :username
//                      and lower(coalesce(u.college, '')) = :college
//                    order by u.createdAt desc
//                    """,
//            countQuery = """
//                    select count(u)
//                    from User u
//                    where u.username <> :username
//                      and lower(coalesce(u.college, '')) = :college
//                    """
//    )
//    Page<String> findDiscoverByCollegeIds(
//            @Param("username") String username,
//            @Param("college") String college,
//            Pageable pageable
//    );
//
//    @Query(
//            value = """
//                    select u.id from User u
//                    where u.username <> :username
//                    order by u.createdAt desc
//                    """,
//            countQuery = """
//                    select count(u) from User u
//                    where u.username <> :username
//                    """
//    )
//    Page<String> findDiscoverFallbackIds(@Param("username") String username, Pageable pageable);
//
//    @Query(
//            value = """
//                    select u.id
//                    from User u
//                    where lower(u.username) like concat('%', :q, '%')
//                       or lower(u.name) like concat('%', :q, '%')
//                    order by u.createdAt desc
//                    """,
//            countQuery = """
//                    select count(u)
//                    from User u
//                    where lower(u.username) like concat('%', :q, '%')
//                       or lower(u.name) like concat('%', :q, '%')
//                    """
//    )
//    Page<String> searchUserIds(@Param("q") String q, Pageable pageable);
//
//    @Query(
//            value = """
//                    select distinct u.id
//                    from User u
//                    join u.userSkills us
//                    join us.skill s
//                    where (lower(u.username) like concat('%', :q, '%')
//                        or lower(u.name) like concat('%', :q, '%'))
//                      and (:type is null or us.type = :type)
//                      and lower(s.skillname) = :skill
//                    order by u.createdAt desc
//                    """,
//            countQuery = """
//                    select count(distinct u)
//                    from User u
//                    join u.userSkills us
//                    join us.skill s
//                    where (lower(u.username) like concat('%', :q, '%')
//                        or lower(u.name) like concat('%', :q, '%'))
//                      and (:type is null or us.type = :type)
//                      and lower(s.skillname) = :skill
//                    """
//    )
//    Page<String> searchUserIdsBySkill(
//            @Param("q") String q,
//            @Param("skill") String skill,
//            @Param("type") SkillType type,
//            Pageable pageable
//    );
//}


package com.tanuj.peerskillexchange.repository;

import com.tanuj.peerskillexchange.enums.SkillType;
import com.tanuj.peerskillexchange.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmailOrUsername(String email, String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    @EntityGraph(attributePaths = {"userSkills", "userSkills.skill"})
    Optional<User> findWithSkillsByUsername(String username);

    @EntityGraph(attributePaths = {"userSkills", "userSkills.skill"})
    @Query("select u from User u where u.id in :ids")
    List<User> findAllWithSkillsByIdIn(@Param("ids") Collection<String> ids);

    @Query(
            value = """
                    select u.id
                    from User u
                    join u.userSkills us
                    join us.skill s
                    where u.username <> :username
                      and us.type = :type
                      and lower(s.skillname) in :skills
                    group by u.id, u.createdAt
                    order by u.createdAt desc
                    """,
            countQuery = """
                    select count(distinct u.id)
                    from User u
                    join u.userSkills us
                    join us.skill s
                    where u.username <> :username
                      and us.type = :type
                      and lower(s.skillname) in :skills
                    """
    )
    Page<String> findDiscoverCandidateIds(
            @Param("username") String username,
            @Param("type") SkillType type,
            @Param("skills") List<String> skills,
            Pageable pageable
    );

    @Query(
            value = """
                    select u.id
                    from User u
                    left join u.userSkills us
                    left join us.skill s
                    where u.username <> :username
                      and (
                            (us.type = :type and lower(s.skillname) in :skills)
                         or lower(coalesce(u.college, '')) = :college
                      )
                    group by u.id, u.college, u.createdAt
                    order by
                      case when lower(coalesce(u.college, '')) = :college then 0 else 1 end,
                      u.createdAt desc
                    """,
            countQuery = """
                    select count(distinct u.id)
                    from User u
                    left join u.userSkills us
                    left join us.skill s
                    where u.username <> :username
                      and (
                            (us.type = :type and lower(s.skillname) in :skills)
                         or lower(coalesce(u.college, '')) = :college
                      )
                    """
    )
    Page<String> findDiscoverBySkillsOrCollegeIds(
            @Param("username") String username,
            @Param("type") SkillType type,
            @Param("skills") List<String> skills,
            @Param("college") String college,
            Pageable pageable
    );

    @Query(
            value = """
                    select u.id
                    from User u
                    where u.username <> :username
                      and lower(coalesce(u.college, '')) = :college
                    order by u.createdAt desc
                    """,
            countQuery = """
                    select count(u)
                    from User u
                    where u.username <> :username
                      and lower(coalesce(u.college, '')) = :college
                    """
    )
    Page<String> findDiscoverByCollegeIds(
            @Param("username") String username,
            @Param("college") String college,
            Pageable pageable
    );

    @Query(
            value = """
                    select u.id from User u
                    where u.username <> :username
                    order by u.createdAt desc
                    """,
            countQuery = """
                    select count(u) from User u
                    where u.username <> :username
                    """
    )
    Page<String> findDiscoverFallbackIds(@Param("username") String username, Pageable pageable);

    @Query(
            value = """
                    select u.id
                    from User u
                    where lower(u.username) like concat('%', :q, '%')
                       or lower(u.name) like concat('%', :q, '%')
                    order by u.createdAt desc
                    """,
            countQuery = """
                    select count(u)
                    from User u
                    where lower(u.username) like concat('%', :q, '%')
                       or lower(u.name) like concat('%', :q, '%')
                    """
    )
    Page<String> searchUserIds(@Param("q") String q, Pageable pageable);

    @Query(
            value = """
                    select u.id
                    from User u
                    join u.userSkills us
                    join us.skill s
                    where (lower(u.username) like concat('%', :q, '%')
                        or lower(u.name) like concat('%', :q, '%'))
                      and (:type is null or us.type = :type)
                      and lower(s.skillname) = :skill
                    group by u.id, u.createdAt
                    order by u.createdAt desc
                    """,
            countQuery = """
                    select count(distinct u.id)
                    from User u
                    join u.userSkills us
                    join us.skill s
                    where (lower(u.username) like concat('%', :q, '%')
                        or lower(u.name) like concat('%', :q, '%'))
                      and (:type is null or us.type = :type)
                      and lower(s.skillname) = :skill
                    """
    )
    Page<String> searchUserIdsBySkill(
            @Param("q") String q,
            @Param("skill") String skill,
            @Param("type") SkillType type,
            Pageable pageable
    );

    @Query(
            value = """
                    select u.id
                    from User u
                    where u.username <> :username
                      and (:q = '' or lower(u.username) like concat('%', :q, '%'))
                    order by
                      case when (
                            exists (
                                select otherTeach.id
                                from UserSkill otherTeach
                                where otherTeach.users = u
                                  and otherTeach.type = :teachType
                                  and otherTeach.skill in (
                                      select meLearn.skill
                                      from UserSkill meLearn
                                      where meLearn.users.username = :username
                                        and meLearn.type = :learnType
                                  )
                            )
                            and exists (
                                select otherLearn.id
                                from UserSkill otherLearn
                                where otherLearn.users = u
                                  and otherLearn.type = :learnType
                                  and otherLearn.skill in (
                                      select meTeach.skill
                                      from UserSkill meTeach
                                      where meTeach.users.username = :username
                                        and meTeach.type = :teachType
                                  )
                            )
                      ) then 0 else 1 end,
                      u.createdAt desc
                    """,
            countQuery = """
                    select count(u)
                    from User u
                    where u.username <> :username
                      and (:q = '' or lower(u.username) like concat('%', :q, '%'))
                    """
    )
    Page<String> findDiscoverOrderedIds(
            @Param("username") String username,
            @Param("q") String q,
            @Param("teachType") SkillType teachType,
            @Param("learnType") SkillType learnType,
            Pageable pageable
    );
}