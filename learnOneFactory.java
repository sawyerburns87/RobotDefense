import jig.misc.rd.ai.AgentFactory;
import jig.misc.rd.ai.RobotDefenseAgent;




public class learnOneFactory implements AgentFactory {

	public RobotDefenseAgent createAgent(String name, String agentResource) {
		return new learnOneAgent();
	}

}
