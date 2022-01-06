package de.example.haegertime.timetables;

import de.example.haegertime.users.User;
import lombok.Data;
import org.sonatype.inject.Nullable;
import org.springframework.validation.annotation.Validated;

import javax.persistence.*;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

@Data
@Entity
@Table(name = "timetable")
@Validated
//TODO: If end or starttime is null the other must be null as well (+breaklength = null +expected and actual hours must be zero) and absence status must not be null
//TODO: If absence staus is not null, all teh duration parameters must be null/zero
public class TimeTableDay {

    @Id
    @SequenceGenerator(
            name = "user_sequence",
            sequenceName = "user_sequence",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "user_sequence"
    )
    private Long workdayId;  //serves as a unique identifier of the object to simplify deleting/editing single datapoints

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name="employee_id", referencedColumnName = "id")
    private User employee;
    private LocalDate date;
    @Nullable
    private LocalTime startTime;
    @Nullable
    private LocalTime endTime;
    @Min(value = 0) @Max(value = 24)
    private double breakLength;
    @Min(value = 0) @Max(value = 24)
    private double expectedHours;
    @Min(value = 0) @Max(value = 24)
    private double actualHours;
    @Nullable
    private AbsenceStatus absenceStatus;
    @Min(value = 0) @Max(value = 24)
    private double holidayHours;
    @Min(value = 0) @Max(value = 24)
    private double sickHours;
    private Long projectId; //We need some sort of validation that project does exist in project DB (Foreign key?)
    private boolean finalized;

    public TimeTableDay() {
    }

    public TimeTableDay(LocalDate date, LocalTime startTime, LocalTime endTime, double breakLength,
                        double expectedHours, AbsenceStatus absenceStatus, Long projectId) {
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;

        this.breakLength = breakLength;
        this.expectedHours = expectedHours;
        this.absenceStatus = absenceStatus;
        this.projectId = projectId;
        this.finalized = false;
        this.actualHours = calculateActualHours();

        if(this.absenceStatus == null){
            this.sickHours = 0;
            this.holidayHours = 0;
        }
        else{
            calculateMissingHours(); // calculates sickness and holiday hours
        }

    }

    public double calculateActualHours() {
        if (this.absenceStatus != null) { //if sick or on holiday, the expected hours are achieved
            double actualHours = expectedHours;
            return actualHours;
        }
        else if(this.absenceStatus == null && this.startTime == null){ //this covers weekends/public holidays
            return 0;
        }
        else { //otherwise calculate from start-,end- and breaktime
            Duration timeAtWork = DatesAndDurationsCalculator.getDurationBetweenLocalTimes(this.endTime, this.startTime);
            //convert Duration to float hours:
            Long minutes = timeAtWork.toMinutes();
            double actualHours = minutes / 60.0 - this.breakLength;
            return actualHours;
        }
    }

    public void calculateMissingHours() {
        if (this.startTime == null && this.absenceStatus == AbsenceStatus.HOLIDAY){ // if employee never started to work
            this.holidayHours = this.expectedHours;
        }
        else if (this.startTime == null && this.absenceStatus == AbsenceStatus.SICK){ // if employee never started to work
            this.sickHours = this.expectedHours;
        }
        //All remaining cases: if employee did start with work...
        else if (this.absenceStatus == AbsenceStatus.HOLIDAY) {
            this.sickHours = 0;
            Duration timeAtWork = DatesAndDurationsCalculator.getDurationBetweenLocalTimes(this.endTime, this.startTime);
            Long minutes = timeAtWork.toMinutes();
            double workedHours = minutes / 60.0 - this.breakLength;
            this.holidayHours = this.expectedHours - workedHours;
        }
        else if(this.absenceStatus == AbsenceStatus.SICK){
            this.holidayHours = 0;
            Duration timeAtWork = DatesAndDurationsCalculator.getDurationBetweenLocalTimes(this.endTime, this.startTime);
            Long minutes = timeAtWork.toMinutes();
            double workedHours = minutes / 60.0 - this.breakLength;
            this.sickHours = this.expectedHours - workedHours;
    }
    }

    public void assignUser(User employee) {
        this.employee = employee;
    }
}
