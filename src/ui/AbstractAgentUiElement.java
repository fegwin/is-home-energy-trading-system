package ui;

import agents.interfaces.Observable;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import ui.containers.StatusContainerBase;
import ui.interfaces.Informable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.SoftBevelBorder;
import java.awt.*;

/**
 * Created by fegwin on 27/09/2016.
 */
public abstract class AbstractAgentUiElement extends JPanel implements Informable {
    protected AgentController agentController;

    private JLabel currentHourOfDay;
    private JLabel currentDayOfWeek;

    AbstractAgentUiElement(AgentController agentController) throws StaleProxyException {
        super();
        this.setLayout(new GridLayout(0, 1));

        this.setBackground(Color.white);

        this.agentController = agentController;
        createLayout();
    }

    protected void createLayout() throws StaleProxyException {
        this.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Add the title
        JLabel title = new JLabel(getAgentSimpleName());
        Font font = title.getFont();
        Font boldFont = new Font(font.getFontName(), Font.BOLD, font.getSize());
        title.setFont(boldFont);

        this.add(title);

        // add timekeeping
        currentHourOfDay = new JLabel();
        currentDayOfWeek = new JLabel();

        this.add(currentDayOfWeek);
        this.add(currentHourOfDay);
    }

    protected String getAgentSimpleName() throws StaleProxyException {
        return agentController.getName().split("@")[0];
    }

    @Override
    public void inform(StatusContainerBase currentStatus) {
        currentDayOfWeek.setText("Day of week: " + currentStatus.dayOfWeek.toString());
        currentHourOfDay.setText("Hour of day: " + currentStatus.hourOfDay);
    }
}
