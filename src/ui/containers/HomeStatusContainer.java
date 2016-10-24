package ui.containers;

import agents.models.Proposal;

import java.time.DayOfWeek;

/**
 * Created by fegwin on 13/10/2016.
 */
public class HomeStatusContainer extends StatusContainerBase {
    public int currentNetConsumption;
    public Proposal currentEnergyContract;

    public HomeStatusContainer(int currentNetConsumption, int hourOfDay, DayOfWeek dayOfWeek, Proposal contract) {
        super(hourOfDay, dayOfWeek);

        this.currentNetConsumption = currentNetConsumption;
        this.currentEnergyContract = contract;
    }
}
