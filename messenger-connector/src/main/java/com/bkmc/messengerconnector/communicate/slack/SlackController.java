package com.bkmc.messengerconnector.communicate.slack;

import com.bkmc.messengerconnector.communicate.IController;
import com.bkmc.messengerconnector.config.Messenger;
import com.bkmc.messengerconnector.message.Message;
import com.bkmc.messengerconnector.messagehub.InMessageHub;
import com.bkmc.messengerconnector.messagehub.messengerhub.SlackMessageHub;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * Project: communicate-Connector
 * ===========================================
 * User: ByeongGil Jung
 * Date: 2018-09-09
 * Time: 오후 5:19
 */
@RestController
public class SlackController implements IController {

    @Autowired
    SlackHttpCallProcess slackHttpCallProcess;

    @Autowired
    InMessageHub inMessageHub;

    @Autowired
    SlackMessageHub slackMessageHub;

    private ObjectMapper mapper = new ObjectMapper();

    @RequestMapping("/slack/event")
    public Map slackEventListener(@RequestBody String body) throws IOException {
        System.out.println(body);

        Message message = new Message();

        HashMap entireMsgJson = mapper.readValue(body, HashMap.class);
        HashMap eventMsgJson = (HashMap) entireMsgJson.get("event");

        if (!eventMsgJson.containsKey("bot_id")) {
            String user = (String) eventMsgJson.get("user");
            String text = (String) eventMsgJson.get("text");
            String displayName;
            String userInfoJson = slackHttpCallProcess.getUserInfo(user);

            HashMap entireUserJson = mapper.readValue(userInfoJson, HashMap.class);
            HashMap userUserJson = (HashMap) entireUserJson.get("user");
            HashMap profileUserJson = (HashMap) userUserJson.get("profile");

            displayName = (String) profileUserJson.get("display_name");

            message.setMessengerFrom(Messenger.SLACK.getValue());
            message.setSender(displayName);
            message.setContext(text);

            System.out.println(displayName);
            System.out.println(text);

            inMessageHub.addMessage(message);
            inMessageHub.handlingMessage();
        }

        return entireMsgJson;
    }

    @RequestMapping("/slack/send")
    public ArrayList slackMessageSender() throws IOException {
        ArrayList msgList = slackMessageHub.pollAllMessage();
        ArrayList formattedMessageList = formattingProcess(msgList);
        System.out.println(formattedMessageList);

        for (Object message : msgList) {
            System.out.println("1" + message);
            slackHttpCallProcess.sendMessageToSlack((Message) message);
            System.out.println("2" + message);
        }

        return formattedMessageList;
    }

    @RequestMapping("/slack/oauth")
    public Map slackOAuthListener(HttpServletRequest request) {
        return request.getParameterMap();
    }

    private ArrayList formattingProcess(ArrayList msgDTOs) {
        ArrayList<HashMap> msgList = new ArrayList<>();

        for (Object msgDTO : msgDTOs) {
            HashMap<String, String> msg = new HashMap<>();

            Message message = (Message) msgDTO;
            msg.put("sender", String.format("%s [%s | %s]", message.getSender(), message.getMessengerFrom(), message.getChannelFrom()));
            msg.put("text", message.getContext());

            msgList.add(msg);
        }

        return msgList;
    }
}