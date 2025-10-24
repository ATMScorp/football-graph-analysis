import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Player {
    private int number;
    private String name;
    private String position;
    private String dateOfBirth;
    private int age;
    private String nationality;
    private String currentClub;
    private String height;
    private String foot;
    private String joined;
    private String signedFrom;
    private String marketValue;
}
