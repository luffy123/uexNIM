package org.zywx.wbpalmstar.plugin.uexnim;

import android.util.Log;

import com.google.gson.Gson;
import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.Observer;
import com.netease.nimlib.sdk.StatusCode;
import com.netease.nimlib.sdk.auth.AuthServiceObserver;
import com.netease.nimlib.sdk.auth.OnlineClient;
import com.netease.nimlib.sdk.msg.MsgServiceObserve;
import com.netease.nimlib.sdk.msg.model.AttachmentProgress;
import com.netease.nimlib.sdk.msg.model.IMMessage;
import com.netease.nimlib.sdk.msg.model.RecentContact;
import com.netease.nimlib.sdk.team.TeamServiceObserver;
import com.netease.nimlib.sdk.team.model.Team;
import com.netease.nimlib.sdk.team.model.TeamMember;

import org.zywx.wbpalmstar.plugin.uexnim.util.NIMConstant;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ListenerRegister {
    private ListenersCallback callback;

    public void registerListeners(){

        // 如果发送的多媒体文件消息，还需要监听文件的上传进度。
        NIMClient.getService(MsgServiceObserve.class).observeAttachmentProgress(new Observer<AttachmentProgress>() {
            @Override
            public void onEvent(AttachmentProgress attachmentProgress) {
                //如果是接收对方发来的图片，音频，视频，也会触发这个回调。只是getTransferred 为 0
                //针对上传
                if (attachmentProgress.getTransferred() != 0) {
                    Log.i("Listener", "upload progress:" + attachmentProgress.getTransferred() + " /" + attachmentProgress.getTotal());
                    HashMap<String, Object> result = new HashMap<String, Object>();
                    result.put("result", true);
                    result.put(NIMConstant.TEXT_PROGRESS, attachmentProgress.getTransferred() / attachmentProgress.getTotal());
                    result.put(NIMConstant.TEXT_MESSAGE_ID, attachmentProgress.getUuid());
                    callback.onSendMessageWithProgress(result);
                }

            }
        }, true);
        //监听接收的消息
        Observer<List<IMMessage>> incomingMessageObserver = new Observer<List<IMMessage>>() {
            @Override
            public void onEvent(List<IMMessage> messages) {
                // 处理新收到的消息，为了上传处理方便，SDK 保证参数 messages 全部来自同一个聊天对象。
                callback.onReceivedMessages(messages);

            }
        };
        NIMClient.getService(MsgServiceObserve.class).observeReceiveMessage(incomingMessageObserver, true);

        // 监听消息状态变化
        Observer<IMMessage> statusObserver = new Observer<IMMessage>() {
            @Override
            public void onEvent(IMMessage msg) {
                callback.onMessageStatusChange(msg);
            }
        };
        NIMClient.getService(MsgServiceObserve.class).observeMsgStatus(statusObserver, true);

        //监听最近会话变更
        Observer<List<RecentContact>> messageObserver = new Observer<List<RecentContact>>() {
            @Override
            public void onEvent(List<RecentContact> messages) {
                callback.onUpdateRecentSession(messages);
            }
        };
        NIMClient.getService(MsgServiceObserve.class).observeRecentContact(messageObserver, true);
        registerTeamObservers(true);

    }
    public void registerLoginStatusListener() {
        //监听登录状态
        NIMClient.getService(AuthServiceObserver.class).observeOnlineStatus(new Observer<StatusCode>() {
            @Override
            public void onEvent(StatusCode statusCode) {
                if (StatusCode.KICK_BY_OTHER_CLIENT == statusCode) {
                    callback.onKick(1);
                } else if (StatusCode.KICKOUT == statusCode) {
                    callback.onKick(3);
                }
            }
        }, true);
    }
    public void registerMultiLoginClientListener() {
        NIMClient.getService(AuthServiceObserver.class).observeOtherClients(new Observer<List<OnlineClient>>() {
            @Override
            public void onEvent(List<OnlineClient> onlineClients) {
                callback.onMultiLoginClientsChanged(onlineClients);
            }
        },true);
    }
    public void registerTeamObservers(boolean register) {
        NIMClient.getService(TeamServiceObserver.class).observeTeamUpdate(teamUpdateObserver, register);
        NIMClient.getService(TeamServiceObserver.class).observeTeamRemove(teamRemoveObserver, register);
        NIMClient.getService(TeamServiceObserver.class).observeMemberUpdate(memberUpdateObserver, register);
        NIMClient.getService(TeamServiceObserver.class).observeMemberRemove(memberRemoveObserver, register);
    }

    // 群资料变动观察者通知。新建群和群更新的通知都通过该接口传递
    private Observer<List<Team>> teamUpdateObserver = new Observer<List<Team>>() {
        @Override
        public void onEvent(List<Team> teams) {
            Log.i("onEvent", "--teamUpdateObserver:" + new Gson().toJson(teams));
        }
    };

    // 移除群的观察者通知。自己退群，群被解散，自己被踢出群时，会收到该通知
    private Observer<Team> teamRemoveObserver = new Observer<Team>() {
        @Override
        public void onEvent(Team team) {
            // team的flag被更新，isMyTeam为false
            Log.i("onEvent", "--teamRemoveObserver:" + new Gson().toJson(team));
        }
    };

    // 群成员资料变化观察者通知。可通过此接口更新缓存。
    private Observer<List<TeamMember>> memberUpdateObserver = new Observer<List<TeamMember>>() {
        @Override
        public void onEvent(List<TeamMember> members) {
            Log.i("onEvent", "--memberUpdateObserver:" + new Gson().toJson(members));
        }
    };

    // 移除群成员的观察者通知。
    private Observer<TeamMember> memberRemoveObserver = new Observer<TeamMember>() {
        @Override
        public void onEvent(TeamMember member) {
            // member的validFlag被更新，isInTeam为false
            Log.i("onEvent", "--memberRemoveObserver:" + new Gson().toJson(member));
        }
    };
    private Observer<IMMessage> statusObserver = new Observer<IMMessage>() {
        @Override
        public void onEvent(IMMessage msg) {
            // 1、根据sessionId判断是否是自己的消息
            // 2、更改内存中消息的状态
            // 3、刷新界面
        }
    };
    public void setCallback(ListenersCallback callback) {
        this.callback = callback;
    }

    public interface ListenersCallback{
        void onSendMessageWithProgress(Map<String, Object> result);
        void onReceivedMessages(List<IMMessage> messages);
        void onMessageStatusChange(IMMessage msg);
        void onUpdateRecentSession(List<RecentContact> messages);

        void onKick(int code);

        void onMultiLoginClientsChanged(List<OnlineClient> onlineClients);
    }
}