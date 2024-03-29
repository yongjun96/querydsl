package jpa.querydsl.Entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@ToString(of = {"id", "username", "age"})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id
    @GeneratedValue
    @Column(name = "member_id")
    private Long id;

    private String username;

    private int age;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    public void changeMember(Team team){
        this.team = team;
        team.getMemberList().add(this);
    }

    public Member(String username, int age) {
        this(username, age, null);
    }

    public Member(String username) {
        this(username, 0);
    }

    public Member(String username, int age, Team team) {
        this.username = username;
        this.age = age;
        if(team != null){
            changeMember(team);
        }
    }
}
