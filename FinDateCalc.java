import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;
public class FinDateCalc {

    public static int getFinDaysBetween(LocalDate startDate, LocalDate endDate) {
        // Initialize the total number of days
        int totalDays = (int) ChronoUnit.DAYS.between(startDate, endDate);

        // Adjust for 31st days
        totalDays -= count31stDays(startDate, endDate);

        // Adjust for missing February days when applicable
        if(isAfterFeb(endDate))
        		totalDays += countMissingFebruaryDays(startDate, endDate);

        return totalDays;
    }

    private static long count31stDays(LocalDate startDate, LocalDate endDate) {
        long count = 0;
        LocalDate date = startDate;

        while (!date.isAfter(endDate)) {
            if (date.getDayOfMonth() == 31) {
                count++;
            }
            date = date.plusDays(1);
        }
        return count;
    }

    private static long countMissingFebruaryDays(LocalDate startDate, LocalDate endDate) {
        long count = 0;
        LocalDate date = startDate;

        // Check each February in the range
        while (!date.isAfter(endDate)) {
            if (date.getMonthValue() == 2) {
                if (date.lengthOfMonth() == 28) {
                    count += 2;
                } else if (date.lengthOfMonth() == 29) {
                    count += 1;
                }
            }
            date = date.plusMonths(1).withDayOfMonth(1);
        }

        return count;
    }
    
    private static boolean isAfterFeb(LocalDate date)
    {
    	if(date.isAfter(LocalDate.of(date.getYear(), Month.FEBRUARY, 1).withDayOfMonth(LocalDate.of(date.getYear(), Month.FEBRUARY, 1).lengthOfMonth())))
    		return true;
    	else
    		return false;
    }
    
    public static LocalDate addFinancialDays(LocalDate startDate, int financialDays) {
        LocalDate date = startDate;
        long addedDays = 0;
        int lengthOfMonth = 0;
        while (addedDays < financialDays)
        {
        	if(date.getMonthValue() != 2)
        		if(date.getDayOfMonth() < 30)
        		{
                	date = date.plusDays(1);
                	addedDays++;
        		}
        		else
        		{
        			if(startDate.lengthOfMonth() == 31)
        			{
        				// Skip the 31st day
                    	date = date.plusDays(2);
                    	addedDays++;
        			}
        			else
        			{
        				date = date.plusDays(1);
        				addedDays++;
        			}
        		}
        	else
        	{
        		// Adjust for February days
        		lengthOfMonth = date.lengthOfMonth();
        		if(date.getDayOfMonth() < lengthOfMonth && addedDays < financialDays)
        		{
        			date = date.plusDays(1);
        			addedDays += 1;
        		}
        		else
        		{
        			date = date.plusDays(1);
            		addedDays +=2;
        		}
        		
				/*
				 * if(lengthOfMonth == 28 && addedDays <= financialDays - 2) { date =
				 * date.plusDays(1); // February has 28 days, so add the missing days addedDays
				 * += 2; // Compensate for the two missing days } else { if(lengthOfMonth == 29
				 * && addedDays <= financialDays - 1) { date = date.plusDays(1); // February has
				 * 29 days, so add one missing day addedDays += 1; // Compensate for the missing
				 * day } }
				 */
        	}
        }
        return date;
    }
    
    public static LocalDate getLastDayToInvest(LocalDate date)
    {
    	LocalDate lastDay = date;
        while(getFinDaysBetween(lastDay, date) < 30)
        	// Increment financialDays if not skipping
            lastDay = lastDay.minusDays(1);
        return lastDay;
    }
}
