import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.time.Month;
import java.time.YearMonth;

public class QuarterInfo {
    
    public static void main(String[] args) {
        String dateStr = "2024-07-04";
        QuarterResult result = getQuarterInfo(dateStr);
        System.out.println("Quarter Start Date: " + result.getQuarterStartDate());
        System.out.println("Quarter End Date: " + result.getQuarterEndDate());
        System.out.println("Quarter: " + result.getQuarter());
        System.out.println("Year: " + result.getYear());
    }
    
    public static QuarterResult getQuarterInfo(String dateStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate date = LocalDate.parse(dateStr, formatter);
        
        int year = date.getYear();
        int month = date.getMonthValue();
        int quarter = (month - 1) / 3 + 1;

        LocalDate quarterStartDate;
        LocalDate quarterEndDate;
        
        switch (quarter) {
            case 1:
                quarterStartDate = LocalDate.of(year, Month.JANUARY, 1);
                quarterEndDate = LocalDate.of(year, Month.MARCH, 31);
                break;
            case 2:
                quarterStartDate = LocalDate.of(year, Month.APRIL, 1);
                quarterEndDate = LocalDate.of(year, Month.JUNE, 30);
                break;
            case 3:
                quarterStartDate = LocalDate.of(year, Month.JULY, 1);
                quarterEndDate = LocalDate.of(year, Month.SEPTEMBER, 30);
                break;
            case 4:
                quarterStartDate = LocalDate.of(year, Month.OCTOBER, 1);
                quarterEndDate = LocalDate.of(year, Month.DECEMBER, 31);
                break;
            default:
                throw new IllegalArgumentException("Invalid quarter: " + quarter);
        }
        
        return new QuarterResult(quarterStartDate, quarterEndDate, quarter, year);
    }
}

class QuarterResult {
    private LocalDate quarterStartDate;
    private LocalDate quarterEndDate;
    private int quarter;
    private int year;

    public QuarterResult(LocalDate quarterStartDate, LocalDate quarterEndDate, int quarter, int year) {
        this.quarterStartDate = quarterStartDate;
        this.quarterEndDate = quarterEndDate;
        this.quarter = quarter;
        this.year = year;
    }

    public LocalDate getQuarterStartDate() {
        return quarterStartDate;
    }

    public LocalDate getQuarterEndDate() {
        return quarterEndDate;
    }

    public int getQuarter() {
        return quarter;
    }

    public int getYear() {
        return year;
    }
}
