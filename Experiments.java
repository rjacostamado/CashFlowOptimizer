import java.io.IOException;
import java.time.LocalDate;

/*
 * estrategia de búsqueda en el árbol, selección de nodo, de variables
 */

public class Experiments {

	public static void main(String[] args) throws IOException {
		/*
		 * Define the start and end date of the horizon planning period as well as
		 * the dates for the interest rates
		 */
		LocalDate startDate = LocalDate.of(2024, 11, 6);
		LocalDate endDate = LocalDate.of(2025, 12, 31);
		LocalDate dateRates = LocalDate.of(2024, 11, 1);
		
		for(int i = 0; i < 1; i++)
		{
			LocalDate hor_end = endDate.withYear(endDate.getYear() + i * 1);
			/*
			 * The investments in Bancolombia span from 30 up to 1799 days and it is over
			 * this time span that the interest rates are defined. If there are 1800 or more
			 * days in the planning horizon, then the model cannot be built as there are not
			 * data available for the corresponding interest rates.
			 */
			int span = FinDateCalc.getFinDaysBetween(startDate, hor_end);
			if (span > 1799) {
				/*
				throw new IllegalArgumentException
				("investment impossible!");
				 */
				System.out.println("Investment impossible!");
				System.out.println("There are " + span + " days in the planning horizon");
				System.out.println("Time span must be less than or equal to 1799 days!");
				System.out.println("Choose an earlier end date!");
				System.out.println("Current end date is: " + hor_end);
				hor_end = FinDateCalc.addFinancialDays(startDate, 1799);
				System.out.println("Latest end date is: " + hor_end);
			}
			else
			{
				/*
				 * the following lines of code identify the interest rates for the
				 * different lengths of virtual investments.
				 */
				CFO.InterestRateLookup();
				CFO.readData("data/BancolombiaInterestRates.csv");

				// Record the start time
				long startTimeOrg = System.nanoTime();
				long endTime = System.nanoTime();
				double duration = 0;

				System.out.println("Running experiment between " + startDate + " and  " + hor_end);
				CFO cfo = new CFO(startDate, hor_end, dateRates);
				cfo.CreateNodeDataBases(startDate, hor_end);
				cfo.setSinkAndSourceNodes();
				int n = cfo.networkNodes.size() - 1;
				cfo.createNetworkArcs(n);
				cfo.setArcsCoeff();

				/*
				 * If there are more than 30 financial days between start and end dates, then it
				 * creates the node databases, sets the sink and source nodes, determine the
				 * network arcs, reads the interest rates data, and finally, it defines and
				 * solves the cash flow optimization problem (cfop). In case there are less than
				 * 30 financial days between start and end dates, then it prints an error
				 * message.
				 */
				if (FinDateCalc.getFinDaysBetween(startDate, hor_end) >= 30)
				{
					
					cfo.optimizeCashFlow();
					
					// System.out.println("Execution time for model set up: " + duration + "
					// nanoseconds");
					System.out.println("Execution time for model set up:" + duration + " seconds");

				} else
					System.out.println("Investment impossible! Time lapse between dates less than 30 days!");

				System.out.println("Execution time for model set up:" + duration + " seconds");

				endTime = System.nanoTime();
				
				// Calculate the time taken in nanoseconds
				duration = endTime - startTimeOrg;

				// Convert the duration to milliseconds (optional)
				duration = duration / 1_000_000_000.0;
				System.out.println("Total execution time: " + duration + " seconds");
			}
		}
	}		
}