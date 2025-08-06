The Cash Flow Optimization Problem

This module provides a solution to the problem of deciding when to invest and for how long in a virtual financial instrument through a bank's digital platform. The model considers the interest rates offered by the bank, which vary depending on the duration of the investment.

The goal is to maximize the investor’s returns while ensuring that their financial requirements are met throughout a defined planning horizon.

Implementation Details and Assumptions

This is an experimental version of the solution.

The model is implemented in Java and solved using IBM CPLEX.

The project includes three main Java classes:

Node: Represents a day in the planning horizon with attributes such as inflow, outflow, and date.

CashFlowModel: Contains the optimization model.

Experimentation: Builds the model instance and executes the optimization.

Output Format

The output is written to a .csv file that serves as a cash flow database.

Each row in the CSV corresponds to a financial event on a specific date within the planning period.

The CSV file includes the following columns:

start_date

days_between

end_date

value

interests

type – indicates the type of event:

"investment": an investment was made on this date.

"balance": the available balance in the bank account.

The filename follows the format:

cfo_between_<start_date>_and_<end_date>.csv


>>>>>>> 52397b622947f1a7047e4123b0fa1c324ee21fb5
