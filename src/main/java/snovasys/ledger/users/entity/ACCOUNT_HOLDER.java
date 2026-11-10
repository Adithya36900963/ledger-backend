package snovasys.ledger.users.entity;



import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "ACCOUNT_HOLDERs")
public class ACCOUNT_HOLDER {

    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE,generator="user_seq")
    @SequenceGenerator(
        name="user_seq",
        sequenceName="user_sequence",
        allocationSize=1

    )
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;


    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ACCOUNT_HOLDERStatus status=ACCOUNT_HOLDERStatus.UNACTIVE;

    public enum ACCOUNT_HOLDERStatus {
        ACTIVE,
        UNACTIVE,
        DELETED
    }
}