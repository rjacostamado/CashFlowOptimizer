/*
 * n is the number of days to be included in the planning horizon.
 * n goes from zero to n, that is, there are n + 1 days to be considered in the model.
 * The number of decision variables is equal to m, which is given by n * (n - 1) / 2.
 * For example, if there are 5 days, that is, n + 1 = 5, meaning that the last day to
 * collect the funds is day 4, then there are 10 decision variables in the model. Under this
 * scenario, if day zero is, say, 2024-8-15, then day 4 is 2024-8-19 and that is the day where
 * the funds will be collected.
 */

import ilog.cplex.*;
import ilog.concert.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class CFO
{
	LocalDate start;
	LocalDate end;
	LocalDate rates;
	
	// Constructor
	public CFO(LocalDate s, LocalDate e, LocalDate r)
	{
		start = s;
		end = e;
		rates = r;
	}


	static NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US);
	HashMap<Integer, Node> networkNodes = new HashMap<>();
	HashMap<LocalDate, Node> nodeDates = new HashMap<>();
	ArrayList<Node> sourceNodes = new ArrayList<>();
	ArrayList<Node> sinkNodes = new ArrayList<>();
	HashMap<Node, ArrayList<Node>> before = new HashMap<>();
	HashMap<Node, ArrayList<Node>> after = new HashMap<>();
	HashMap<Node, ArrayList<Node>> befInt = new HashMap<>();
	HashMap<Node, ArrayList<Node>> aftInt = new HashMap<>();
	HashMap<Node, ArrayList<Node>> arcs = new HashMap<>();
	HashMap<Node, ArrayList<Node>> intArcs = new HashMap<>();
	HashMap<Node, HashMap<Node, Double>> arcsCoeff = new HashMap<>();
	/*
	 * Sets the time window for the cash flow optimization program (cfop) and the
	 * date of the interest rates to apply in the model. The investment horizon
	 * planning period is defined between startDate and endDate. The date from which
	 * the rates of interest are taken is dateRates
	 *
	LocalDate startDate = start;
	LocalDate endDate = end;
	LocalDate dateRates = rates;
	/*
	 * sets an upper bound for any investment
	 */
	static int M = 13000000;
	/*
	 * sets the day of the month the salary is paid. Cannot be 31, use any day
	 * between 1 and 28. Otherwise, an adjustment is required for the month of
	 * february.
	 */
	static int payDay = 1;
	/*
	 * sets the day passive income (e.g.: rent from a owned property) as the fifth
	 * day of each month.
	 */
	static int pasIncDay = 5;
	/*
	 * the following lines of code set the days for the payment of the
	 * administration, credit cards, water and gas bills and mortgage installment.
	 * For simplicity, remember not to use any day between 29 and 31.
	 */
	static int admPymt = 10;
	static int credCardsPymt = 15;
	static int utilitiesPymt = 16;
	static int mortgagePymt = 25;
	/*
	 * the following lines of code set the month of the year when inflation
	 * increases on the passive income (passIncr), salary (salIncr, administration, credit cards and 
	 * utilities are effective.
	 */
	static int passIncr = 1;
	static int salIncr = 1;	
	static int admIncr = 1;
	static int credCardIncr = 1;
	static int utilIncr = 1;

	static LocalDate lastDate;
	static LocalDate currDate;
	static FinDateCalc calc = new FinDateCalc();
	public static HashMap<Integer, ArrayList<Integer>> alpha = new HashMap<Integer, ArrayList<Integer>>();
	public static HashMap<Integer, ArrayList<Integer>> beta = new HashMap<Integer, ArrayList<Integer>>();

	/*
	 * The following lines of code set the corresponding amounts for passive
	 * income (passIncm), administration payment (admon), credit cards (credCards),
	 *  utilities (util), salary (sal), mortgage, rate of inflation (infl) and
	 *  the interest rate applied on the balance of a savings account (savInt). 
	 */
	public static double passIncm = 1000; // el 5
	static double admon = -400; // el 10
	static double credCards = -8000; // el 15
	static double util = -35; // el 16	
	static double sal = 12500; // el 22
	static double mortgage = -1750; // el 23
	static double infl = 0.05;
	static double savInt = 0;

	public static HashMap<String, Double> flows = new HashMap<String, Double>();
	static int curr_year;
	static int years;
	static int startTime = 0;
	static int size = 0;

	public void CreateNodeDataBases(LocalDate startDate, LocalDate endDate) {
		LocalDate currentDate = startDate;
		Integer nodNum = 0;
		networkNodes.put(nodNum, new Node(0, 0, 0, nodNum, currentDate));
		nodeDates.put(currentDate, networkNodes.get(nodNum));
		updateSourceAndSinkLists(currentDate);

		currentDate = currentDate.plusDays(1);
		nodNum += 1;
		if (startDate.getMonthValue() <= 8) {
			while (currentDate.isBefore(endDate)) {
				while (currentDate.getMonthValue() <= 8 && currentDate.isBefore(endDate)) {
					networkNodes.put(nodNum, new Node(0, 0, 0, nodNum, currentDate));
					nodeDates.put(currentDate, networkNodes.get(nodNum));
					updateSourceAndSinkLists(currentDate);
					currentDate = currentDate.plusDays(1);
					nodNum += 1;
				}
				while (currentDate.getMonthValue() > 8 && currentDate.isBefore(endDate)) {
					networkNodes.put(nodNum, new Node(0, 0, 0, nodNum, currentDate));
					nodeDates.put(currentDate, networkNodes.get(nodNum));
					updateSourceAndSinkLists(currentDate);
					currentDate = currentDate.plusDays(1);
					nodNum += 1;
				}
			}
		} else {
			while (currentDate.isBefore(endDate)) {
				while (currentDate.getMonthValue() > 8 && currentDate.isBefore(endDate)) {
					networkNodes.put(nodNum, new Node(0, 0, 0, nodNum, currentDate));
					nodeDates.put(currentDate, networkNodes.get(nodNum));
					updateSourceAndSinkLists(currentDate);
					currentDate = currentDate.plusDays(1);
					nodNum += 1;
				}
				while (currentDate.getMonthValue() <= 8 && currentDate.isBefore(endDate)) {
					networkNodes.put(nodNum, new Node(0, 0, 0, nodNum, currentDate));
					nodeDates.put(currentDate, networkNodes.get(nodNum));
					updateSourceAndSinkLists(currentDate);
					currentDate = currentDate.plusDays(1);
					nodNum += 1;
				}
			}
		}

		networkNodes.put(nodNum, new Node(0, 0, 0, nodNum, currentDate));
		nodeDates.put(currentDate, networkNodes.get(nodNum));
	}

	private void updateSourceAndSinkLists(LocalDate date) {
		/*
		 * Updates the array list of source nodes
		 */
		if (date.getDayOfMonth() == pasIncDay || date.getDayOfMonth() == payDay)
			sourceNodes.add(nodeDates.get(date));

		/*
		 * Updates the array list of sink nodes
		 */
		if (date.getDayOfMonth() == admPymt || // administración
				date.getDayOfMonth() == credCardsPymt || // tdc's
				date.getDayOfMonth() == utilitiesPymt || // agua y gas
				date.getDayOfMonth() == mortgagePymt) // crédito hipotecario
			sinkNodes.add(nodeDates.get(date));
	}

	public void setSinkAndSourceNodes() {
		/*
		 * sets source nodes
		 */
		for (Node nod : sourceNodes)
			if (nod.getDate().getDayOfMonth() == pasIncDay)
				// nod.setInflow(getCurrentRent(nod));
				nod.setNetFlow(getCurrentPassInc(nod));
			else {
				nod.setNetFlow(getCurrentSalary(nod));
				if (nod.getDate().getMonthValue() == 6
						// || nod.getDate().getMonthValue() == 11 | nod.getDate().getMonthValue() == 12)
						| nod.getDate().getMonthValue() == 12)
					
					nod.setNetFlow(nod.getNetFlow() + 0.5 * sal);
				// System.out.println(nod.getNetFlow());
			}

		/*
		 * sets sink nodes
		 */
		for (Node nod : sinkNodes) {
			if (nod.getDate().getDayOfMonth() == admPymt)
				nod.setNetFlow(admon);
			if (nod.getDate().getDayOfMonth() == credCardsPymt)
				nod.setNetFlow(getCurrentTdc(nod));
			if (nod.getDate().getDayOfMonth() == utilitiesPymt)
				nod.setNetFlow(getCurrentUtil(nod));
			if (nod.getDate().getDayOfMonth() == mortgagePymt)
				nod.setNetFlow(mortgage);
		}
	}

	private static double getCurrentSalary(Node node) {
		if (curr_year != node.getDate().getYear())
			curr_year = node.getDate().getYear();

		if (node.getDate().getMonthValue() == salIncr)
			sal = (1 + infl) * sal;

		// String formattedSalary = currencyFormatter.format(sal);
		// System.out.println("Salary for " + node.getDate() + " is " +
		// formattedSalary);

		return sal;
	}

	private static double getCurrentPassInc(Node node) {
		if (curr_year != node.getDate().getYear())
			curr_year = node.getDate().getYear();

		if (node.getDate().getMonthValue() == passIncr)
			passIncm = (1 + infl) * passIncm;

		// String formattedRent = currencyFormatter.format(rent);
		// System.out.println("Rent for " + node.getDate() + " is " + formattedRent);

		return passIncm;
	}

	private static double getCurrentTdc(Node node) {
		if (curr_year != node.getDate().getYear())
			curr_year = node.getDate().getYear();

		if (node.getDate().getMonthValue() == credCardIncr)
			credCards = (1 + infl) * credCards;

		// String formattedRent = currencyFormatter.format(rent);
		// System.out.println("Rent for " + node.getDate() + " is " + formattedRent);

		return credCards;

	}

	private static double getCurrentUtil(Node node) {
		if (curr_year != node.getDate().getYear())
			curr_year = node.getDate().getYear();

		if (node.getDate().getMonthValue() == utilIncr)
			util = (1 + infl) * util;

		// String formattedRent = currencyFormatter.format(rent);
		// System.out.println("Rent for " + node.getDate() + " is " + formattedRent);

		return util;
	}

	public void createNetworkArcs(int n) {
		LocalDate t1 = start;
		LocalDate to = t1.plusDays(1);
		LocalDate lastInv = FinDateCalc.getLastDayToInvest(end);
		LocalDate t2 = FinDateCalc.addFinancialDays(start, 30);
		int cycles = 0;
		while (!t2.isAfter(end)) {
			t1 = start.plusDays(cycles);
			to = t1.plusDays(1);
			while (!t1.isAfter(t2)) {
				if (to.isBefore(t2)) {
					intArcs.put(nodeDates.get(t1), new ArrayList<Node>());
					while (to.isBefore(t2)) {
						intArcs.get(nodeDates.get(t1)).add(nodeDates.get(to));
						updateNDB(nodeDates.get(t1), nodeDates.get(to), befInt, aftInt);
						to = to.plusDays(1);
					}
					t1 = t1.plusDays(1);
					to = t1.plusDays(1);
				} else {
					t1 = t1.plusDays(1);

				}
			}
			t2 = t2.plusDays(1);
			cycles++;
		}
		t1 = lastInv;
		to = t1.plusDays(1);
		while (t1.isBefore(end)) {
			intArcs.put(nodeDates.get(t1), new ArrayList<Node>());
			while (!to.isAfter(end)) {
				intArcs.get(nodeDates.get(t1)).add(nodeDates.get(to));
				updateNDB(nodeDates.get(t1), nodeDates.get(to), befInt, aftInt);
				to = to.plusDays(1);
			}
			t1 = t1.plusDays(1);
			to = t1.plusDays(1);
		}
		t1 = start;
		while (!t1.isAfter(lastInv)) {
			arcs.put(nodeDates.get(t1), new ArrayList<Node>());
			to = FinDateCalc.addFinancialDays(t1, 30);
			while (!to.isAfter(end)) {
				updateNDB(nodeDates.get(t1), nodeDates.get(to), before, after);
				arcs.get(nodeDates.get(t1)).add(nodeDates.get(to));
				to = to.plusDays(1);
			}
			t1 = t1.plusDays(1);
		}
	}

	private static void updateNDB(Node from, Node to, HashMap<Node, ArrayList<Node>> beta,
			HashMap<Node, ArrayList<Node>> alpha) {
		if (!beta.containsKey(to)) {
			beta.put(to, new ArrayList<Node>());
			beta.get(to).add(from);
		} else {
			if (!beta.get(to).contains(from))
				beta.get(to).add(from);
		}
		if (!alpha.containsKey(from)) {
			alpha.put(from, new ArrayList<Node>());
			alpha.get(from).add(to);
		} else {
			if (!alpha.get(from).contains(to))
				alpha.get(from).add(to);
		}
	}

	public static void readData(String path) throws IOException {
		String row;
		String splitBy = ",";
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy");

		File data = new File(path);
		try (BufferedReader csvReader = new BufferedReader(new FileReader(data))) {

			// Read the header (dates)
			String header = csvReader.readLine();
			if (header != null) {
				String[] dates = header.split(splitBy);

				// Parse the rest of the rows
				while ((row = csvReader.readLine()) != null) {
					String[] csvData = row.split(splitBy);

					// Extract bounds
					int dLinf = Integer.parseInt(csvData[0]);
					int dLsup = Integer.parseInt(csvData[1]);

					// Iterate over interest rates and dates
					for (int i = 4; i < csvData.length; i++) {
						LocalDate date = LocalDate.parse(dates[i], formatter);
						double rate = Double.parseDouble(csvData[i]);

						// Add interest rate to the map for each duration in the range
						for (int duration = dLinf; duration <= dLsup; duration++) {
							interestRates.computeIfAbsent(duration, k -> new HashMap<>()).put(date, rate);
						}
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void optimizeCashFlow() throws IOException {
		
		try (IloCplex smartSaver = new IloCplex()) {

			HashMap<String, IloNumVar> xVars = new HashMap<>();
			HashMap<String, IloNumVar> yVars = new HashMap<>();
			HashMap<String, IloIntVar> zVars = new HashMap<>();
			
			int priority = 0;
			// Set the branching direction mode to prioritize user-defined priorities
			smartSaver.setParam(IloCplex.Param.MIP.Strategy.VariableSelect, 3);
			
			// Set CPLEX to use the network simplex method
            // smartSaver.setParam(IloCplex.Param.RootAlgorithm, IloCplex.Algorithm.Network);
                        
            // Set CPLEX to use the dual simplex method
            // smartSaver.setParam(IloCplex.Param.RootAlgorithm, IloCplex.Algorithm.Dual);

			// Iterate over the arcs map
			for (Node dayFrom : arcs.keySet()) {
				for (Node dayTo : arcs.get(dayFrom)) {
					// Create the variable names based on the arc
					String amount_to_invest = "x_" + dayFrom.getIndex() + "_" + dayTo.getIndex();
					String invest_or_not = "z_" + dayFrom.getIndex() + "_" + dayTo.getIndex();

					// Create the variables and store them in the maps
					IloNumVar xVar = smartSaver.numVar(0, Double.MAX_VALUE, amount_to_invest);
					IloIntVar zVar = smartSaver.intVar(0, 1, invest_or_not);
					
					// Set priority based on the difference in node indices
			        // priority = dayTo.getIndex() - dayFrom.getIndex();

			        // Add the variables to your model before setting the priority
			        smartSaver.add(xVar);
			        smartSaver.add(zVar);

			        // Set the branching priority for zVar
			        // smartSaver.setPriority(zVar, priority);
					
					// Adds the just created variables to their corresponding hashmaps
					xVars.put(amount_to_invest, xVar);
					zVars.put(invest_or_not, zVar);
				}
			}

			for (Node dayFrom : intArcs.keySet()) {
				for (Node dayTo : intArcs.get(dayFrom)) {
					// Create the variable names based on the arc
					String amount_in_balance = "y_" + dayFrom.getIndex() + "_" + dayTo.getIndex();

					// Create the variables and store them in the maps
					IloNumVar yVar = smartSaver.numVar(0, Double.MAX_VALUE, amount_in_balance);
					// System.out.println(yVar);
					
					smartSaver.add(yVar);

					yVars.put(amount_in_balance, yVar);
				}
			}

			int numVars = (networkNodes.size() * (networkNodes.size() - 1)) / 2;
			IloLinearNumExpr objectiveFunction = smartSaver.linearNumExpr();
			IloLinearNumExpr[] consflow = new IloLinearNumExpr[networkNodes.size()];
			IloLinearNumExpr[] inv_or_not_1 = new IloLinearNumExpr[numVars];
			IloLinearNumExpr[] inv_or_not_2 = new IloLinearNumExpr[numVars];

			for (int idx = 0; idx < networkNodes.size() - 1; idx++) {
				Node nod = networkNodes.get(idx);
				consflow[nod.getIndex()] = smartSaver.linearNumExpr();
				/*
				 * Add the variables for which no investment can be made as there are less than
				 * 30 financial days between these dates and the startDate. First it adds up the
				 * outgoing flows and then it substracts the incoming flows, each multiplied by
				 * the rate of interest that corresponds to a savings account; which for the
				 * case of Bancolombia, according to google, 0.1% per year depending on the
				 * balance. For modeling simplicity, it will be taken as a flat rate independent
				 * of the balance.
				 */

				if (aftInt.keySet().contains(nod)) {
					for (Node to : aftInt.get(nod))
						consflow[nod.getIndex()].addTerm(1, yVars.get("y_" + nod.getIndex() + "_" + to.getIndex()));
				}
				if (befInt.keySet().contains(nod)) {
					for (Node from : befInt.get(nod))
						consflow[nod.getIndex()].addTerm(-1, yVars.get("y_" + from.getIndex() + "_" + nod.getIndex()));

				}
				/*
				 * Adds the variables for which an investment can be made, which are those
				 * representing the arcs linking nodes 30 or more financial days appart.
				 */
				if (after.keySet().contains(nod)) {
					for (Node to : after.get(nod))
						consflow[nod.getIndex()].addTerm(1, xVars.get("x_" + nod.getIndex() + "_" + to.getIndex()));
				}
				if (before.keySet().contains(nod)) {
					for (Node from : before.get(nod))
						consflow[nod.getIndex()].addTerm(-arcsCoeff.get(from).get(nod),
								xVars.get("x_" + from.getIndex() + "_" + nod.getIndex()));
				}
			}

			for (int idx = 0; idx < networkNodes.size() - 1; idx++) {
				Node nod = networkNodes.get(idx);
				smartSaver.addEq(nod.getNetFlow(), consflow[nod.getIndex()], "consFlow[" + nod.getIndex() + "]");
			}

			/*
			 * Defines the constraints either or for the investment
			 */
			int i = 0;
			inv_or_not_1[i] = smartSaver.linearNumExpr();
			inv_or_not_2[i] = smartSaver.linearNumExpr();
			for (Node m : after.keySet())
				for (Node n : after.get(m)) {
					inv_or_not_1[i].addTerm(1, xVars.get("x_" + m.getIndex() + "_" + n.getIndex()));
					inv_or_not_2[i].addTerm(-1, xVars.get("x_" + m.getIndex() + "_" + n.getIndex()));
					inv_or_not_1[i].addTerm(-M, zVars.get("z_" + m.getIndex() + "_" + n.getIndex()));
					inv_or_not_2[i].addTerm(M, zVars.get("z_" + m.getIndex() + "_" + n.getIndex()));
					smartSaver.addGe(0, inv_or_not_1[i]);
					smartSaver.addGe(M - 500000, inv_or_not_2[i]);
					i++;
					inv_or_not_1[i] = smartSaver.linearNumExpr();
					inv_or_not_2[i] = smartSaver.linearNumExpr();
				}
			/*
			 * Defines the objective function which is composed by the sum of the money
			 * received the last day of the horizon planning period; which is the sum of all
			 * the flows arriving at the last node from all the previous nodes.
			 */
			Node endPlan = nodeDates.get(end);
			// System.out.println(nodeDates.get(endDate).getIndex());
			for (Node n : before.get(endPlan))
				objectiveFunction.addTerm(arcsCoeff.get(n).get(endPlan),
						xVars.get("x_" + n.getIndex() + "_" + endPlan.getIndex()));
			if (befInt.containsKey(endPlan))
				for (Node n : befInt.get(endPlan))
					objectiveFunction.addTerm(1, yVars.get("y_" + n.getIndex() + "_" + endPlan.getIndex()));

			smartSaver.addMaximize(objectiveFunction);
			
			// Possible values for each parameter
            int[] branchingStrategies = {0, 1, 2};  // e.g., 0: auto, 1: down, 2: up
            int[] nodeSelections = {0, 1, 2};       // 0: depth-first, 1: best-bound, 2: best-estimate
            int[] cutStrategies = {-1, 0, 1, 2, 3}; // -1: none, 0: auto, 1+: more aggressive cuts
            int[] heuristicFreqs = {-1, 0, 5, 10};  // -1: disable, 0: auto, 5, 10: every N nodes
            int[] threadCounts = {1, 2, 4};         // Number of threads to use
            double[] mipgaps = {0.01, 0.001, 0.0001}; // Tolerance for optimality gap
            int[] mipEmphasis = {0, 1, 2, 3, 4};    // 0: balanced, 1: hidden feasible, etc.
            boolean[] presolveSettings = {true, false}; // Enable/Disable presolve

            // Iterate over all combinations
            for (int branch : branchingStrategies) {
                for (int nodeSel : nodeSelections) {
                    for (int cut : cutStrategies) {
                        for (int heuristicFreq : heuristicFreqs) {
                            for (int threads : threadCounts) {
                                for (double mipgap : mipgaps) {
                                    for (int emphasis : mipEmphasis) {
                                        for (boolean presolve : presolveSettings) {

                                            // Set the parameters with correct paths
                                            smartSaver.setParam(IloCplex.Param.MIP.Strategy.Branch, branch);
                                            smartSaver.setParam(IloCplex.Param.MIP.Strategy.NodeSelect, nodeSel);  // Corrected
                                            smartSaver.setParam(IloCplex.Param.MIP.Cuts.Gomory, cut);
                                            smartSaver.setParam(IloCplex.Param.MIP.Strategy.HeuristicFreq, heuristicFreq);
                                            smartSaver.setParam(IloCplex.Param.Threads, threads);
                                            smartSaver.setParam(IloCplex.Param.MIP.Tolerances.MIPGap, mipgap);
                                            smartSaver.setParam(IloCplex.Param.MIP.Display, emphasis);  // Corrected
                                            smartSaver.setParam(IloCplex.Param.Preprocessing.Presolve, presolve);
                                            double startTime = System.nanoTime();
                                            double duration = 0;
                                            
                                            // Run the model
                                            if (smartSaver.solve()) {
                                            	double endTime = System.nanoTime();
                            					// Calculate the time taken in nanoseconds
                            					duration = endTime - startTime;

                            					// Convert the duration to milliseconds (optional)
                            					duration = duration / 1_000_000_000.0;

                                                double objectiveValue = smartSaver.getObjValue();
                                                System.out.println("Objective: " + objectiveValue);
                                            } else {
                                                System.out.println("No solution found with current parameters.");
                                            }
                                            // Log or store results for this combination
                                            //logResults(branch, nodeSel, cut, heuristicFreq, threads, mipgap, emphasis, presolve, duration);
                                            logResultsToCSV(branch, nodeSel, cut, heuristicFreq, threads, mipgap, emphasis, presolve, duration);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            

            // Dispose of the CPLEX object
            smartSaver.end();
			/*
			long startT = System.nanoTime();
			smartSaver.solve();
			long endT = System.nanoTime();
			System.out.println(endT - startT + " seconds to solve the problem");
			System.out.println("Cplex status is: " + smartSaver.getCplexStatus());
			System.out.println("Cplex time is: " + smartSaver.getCplexTime());

			
			
			if (smartSaver.solve()) {
				System.out.println("attempting to create csv file with optimized cash flow");
				/*
				 * cfo stands for cash flow optimization. This code creates a csv with the results
				 * of the cash flow optimization process. It first lists the information of
				 * the investments and then it provides the information of the balance
				 * of the savings account.
				 */
				try (BufferedWriter writer = new BufferedWriter(new FileWriter("cfo_between_" + start + "_and_" + end + ".csv"))) {
				    // Write CSV header
				    writer.write("start_date, days_between, end_date, value, interests, type");
				    writer.newLine(); // Move to the next line
				    
				    DecimalFormat df = new DecimalFormat("#.##"); // Format for the values
				    
				    // Process 'arcs' and 'xVars'
				    List<Node> sortedDayFromNodes = arcs.keySet().stream()
				                                         .sorted((n1, n2) -> n1.getDate().compareTo(n2.getDate()))
				                                         .collect(Collectors.toList());
				    
				    for (Node dayFrom : sortedDayFromNodes) {
				        List<Node> sortedDayToNodes = arcs.get(dayFrom).stream()
				                                           .sorted((n1, n2) -> n1.getDate().compareTo(n2.getDate()))
				                                           .collect(Collectors.toList());
				        
				        for (Node dayTo : sortedDayToNodes) {
				            double value = smartSaver.getValue(xVars.get("x_" + dayFrom.getIndex() + "_" + dayTo.getIndex()));
				            if (value > 1) {
				                String startDate = dayFrom.getDate().toString();
				                String endDate = dayTo.getDate().toString();
				                int daysBetween = FinDateCalc.getFinDaysBetween(dayFrom.getDate(), dayTo.getDate());
				                double coeffValue = (arcsCoeff.get(dayFrom).get(dayTo) - 1) * value;
				                
				                // Write the line to the CSV file
				                writer.write(startDate + "," + daysBetween + "," + endDate + "," + df.format(value) + "," + df.format(coeffValue) + "," + "investment");
				                writer.newLine();
				            }
				        }
				    }
				    
				    // Process 'intArcs' and 'yVars'
				    List<Node> sortedIntDayFromNodes = intArcs.keySet().stream()
				                                              .sorted((n1, n2) -> n1.getDate().compareTo(n2.getDate()))
				                                              .collect(Collectors.toList());
				    
				    for (Node dayFrom : sortedIntDayFromNodes) {
				        List<Node> sortedIntDayToNodes = intArcs.get(dayFrom).stream()
				                                                .sorted((n1, n2) -> n1.getDate().compareTo(n2.getDate()))
				                                                .collect(Collectors.toList());
				        
				        for (Node dayTo : sortedIntDayToNodes) {
				            double yValue = smartSaver.getValue(yVars.get("y_" + dayFrom.getIndex() + "_" + dayTo.getIndex()));
				            if (yValue > 1) {
				                String startDate = dayFrom.getDate().toString();
				                String endDate = dayTo.getDate().toString();
				                int daysBetween = FinDateCalc.getFinDaysBetween(dayFrom.getDate(), dayTo.getDate());
				                double coeffValue = getArcCoeff(dayFrom.getDate(), dayTo.getDate(), rates) * yValue;
				                
				                // Write the line for intArcs to the CSV file
				                writer.write(startDate + "," + daysBetween + "," + endDate + "," + df.format(yValue) + "," + df.format(coeffValue) + "," + "balance");
				                writer.newLine();
				            }
				        }
				    }
				    
				    System.out.println("CSV file 'cfo_between_" + start + "_and_" + end + ".csv\"' created successfully.");
				    
					System.out.println("Objective function = " + df.format(smartSaver.getValue(objectiveFunction)));
				}
		} catch (IloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void logResultsToCSV(int branch, int nodeSel, int cut, int heuristicFreq, int threads, double mipgap, int emphasis, boolean presolve, double duration) {
	    String csvFile = "experiment_results.csv";
	    File file = new File(csvFile);
	    boolean isFileNew = !file.exists();  // Check if file exists

	    try (FileWriter fileWriter = new FileWriter(csvFile, true); // Append mode
	         PrintWriter printWriter = new PrintWriter(fileWriter)) {

	        // If the file is new or empty, write the header first
	        if (isFileNew || file.length() == 0) {
	            printWriter.println("Branch,NodeSel,Cut,HeuristicFreq,Threads,MipGap,Emphasis,Presolve,Duration");
	        }

	        // Write the data
	        printWriter.printf("%d,%d,%d,%d,%d,%.6f,%d,%b,%.6f\n",
	                branch, nodeSel, cut, heuristicFreq, threads, mipgap, emphasis, presolve, duration);

	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}

	
	private void logResults(int branch, int nodeSel, int cut, int heuristicFreq, int threads, double mipgap, int emphasis, boolean presolve, double duration) {
        System.out.printf("Branch: %d, NodeSel: %d, Cut: %d, HeuristicFreq: %d, Threads: %d, MipGap: %.6f, Emphasis: %d, Presolve: %b, Duration: %f\n",
                branch, nodeSel, cut, heuristicFreq, threads, mipgap, emphasis, presolve, duration);
    }

	// Nested Map to store the data: outer map key is the lower bound of duration
	private static Map<Integer, Map<LocalDate, Double>> interestRates;

	public static void InterestRateLookup() {
		interestRates = new HashMap<>();
	}

	// Method to get the interest rate based on a date they were registered and
	// duration
	public static Double getInterestRate(LocalDate date, int duration) {
		Map<LocalDate, Double> ratesForDuration = interestRates.get(duration);
		if (ratesForDuration != null) {
			return ratesForDuration.get(date);
		}
		return null; // or throw an exception or return a default value
	}

	/*
	 * works out the arc coefficients using the csv with the interest rates from
	 * bancolombia As there are several interest rates for different dates as they
	 * have changed them, then it receives the LocalDate date to determine which of
	 * the different lists of interest rates it will use in the computations. It
	 * also takes two additional arguments: start and end. These two LocalDates are
	 * the beginning and the ending of the period of a given investment.
	 */
	public static double getArcCoeff(LocalDate start, LocalDate end, LocalDate date) {
		double coef = 0;
		int duration = FinDateCalc.getFinDaysBetween(start, end);
		if (duration > 29)
			coef = Math.pow(1 + getInterestRate(date, duration), ((double) duration / 360));
		else
			coef = Math.pow(1.001, duration / 360) - 1;
		return coef;
	}

	public void setArcsCoeff() {
		for (Node from : arcs.keySet()) {
			arcsCoeff.put(from, new HashMap<Node, Double>());
			for (Node to : arcs.get(from)) {
				LocalDate s = from.getDate();
				LocalDate e = to.getDate();
				arcsCoeff.get(from).put(to, getArcCoeff(s, e, rates));
			}
		}
	}

	public void printNetworkArcs() {
		System.out.println("This is the list of arcs created for the network");
		for (Node from : arcs.keySet()) {
			System.out.print("from " + from.getIndex() + "\t");
			for (Node to : arcs.get(from))
				System.out.print("\t" + to.getIndex());
			System.out.println();
		}
		System.out.println("End of list of network arcs in the network");
	}

	public void printArcsCoefficients() {
		System.out.println("This are the arcs for which coefficients have been created for the network");
		for (Node from : arcsCoeff.keySet()) {
			System.out.print("from " + from.getIndex());
			for (Node to : arcsCoeff.get(from).keySet())
				System.out.print(" to " + to.getIndex() + " ");
			System.out.println();
			System.out.print(from.getDate() + "\t" + " to ");
			for (Node to : arcsCoeff.get(from).keySet())
				System.out.print(to.getDate() + "\t");
			System.out.println();
			for (Node to : arcsCoeff.get(from).keySet())
				System.out.print(arcsCoeff.get(from).get(to) + "\t");
			System.out.println();
		}
		System.out.println("End of list of network arcs in arcsCoeff");
		System.out.println();
	}
}
