package ui;

import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import ui.containers.HomeStatusContainer;
import ui.containers.StatusContainerBase;

import java.awt.*;
import javax.swing.*;

/**
 * Created by Aswin Lakshman on 27/09/2016.
 */
public class HomeAgentUiElement extends AbstractAgentUiElement {
    private JLabel currentNetConsumption;
    private JLabel totalSpendToDate;
    private JLabel currentContractDetails;

    private GraphPanel usageGraph;
    private GraphPanel predictionGraph;
    private double[] usageGraphScores = new double[24*7];
    private double[] predictionGraphScores = new double[24*7];

    public HomeAgentUiElement(AgentController agentController) throws StaleProxyException {
        super(agentController, true, false);

        currentNetConsumption = new JLabel();
        totalSpendToDate = new JLabel();
        currentContractDetails = new JLabel();

        this.add(currentNetConsumption, getGridBagConstraints());
        this.add(totalSpendToDate, getGridBagConstraints());
        this.add(currentContractDetails, getGridBagConstraints());

        usageGraph = new GraphPanel(usageGraphScores);
        this.add(usageGraph, getGraphGridBagConstraints());

        predictionGraph = new GraphPanel(predictionGraphScores);
        this.add(predictionGraph, getGraphGridBagConstraints());
    }
    
    private int timeToGraph(HomeStatusContainer status) {
    	return (status.dayOfWeek.getValue()-1) *24 + status.hourOfDay;
    }

    @Override
    public void inform(StatusContainerBase currentStatus) {
        super.inform(currentStatus);
        HomeStatusContainer status = (HomeStatusContainer)currentStatus;

        currentNetConsumption.setText(String.format("Current net: %.2fkwH", status.currentNetConsumption));
        totalSpendToDate.setText(String.format("Total spend: $%.2f", status.totalSpendToDate));

        int graphIndex = timeToGraph(status);
        if(status.currentEnergyContract != null) {
            currentContractDetails.setText(String.format("Contract with %s.  Buying@$%.2f.  Selling@$%.2f.  Duration: %s hours",
                    status.currentEnergyContract.retailer,
                    status.currentEnergyContract.retailerSellingPrice,
                    status.currentEnergyContract.retailerBuyingPrice,
                    status.currentEnergyContract.duration));
            currentContractDetails.validate();

            usageGraph.setStartEndTimes(graphIndex, graphIndex + status.currentEnergyContract.duration);
        }

        usageGraphScores[(status.dayOfWeek.getValue()-1) *24 + status.hourOfDay] = ((double)status.currentNetConsumption);
        usageGraph.setScores(usageGraphScores);
        usageGraph.setCurrentTime(graphIndex);

        for(int i = 0; i < status.graphScoresPrediction.length; i++) {
            predictionGraphScores[i] = status.graphScoresPrediction[i];
        }

        predictionGraph.setScores(predictionGraphScores);
    }

    private GridBagConstraints getGraphGridBagConstraints() {
        GridBagConstraints cons = new GridBagConstraints();

        cons.fill = GridBagConstraints.BOTH;
        cons.weightx = 1;
        cons.weighty = 1;
        cons.gridx = 0;

        return cons;
    }
}
