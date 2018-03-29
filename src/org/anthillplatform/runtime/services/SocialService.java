package org.anthillplatform.runtime.services;

import org.anthillplatform.runtime.AnthillRuntime;
import org.anthillplatform.runtime.Status;
import org.anthillplatform.runtime.entity.AccessToken;
import org.anthillplatform.runtime.request.JsonRequest;
import org.anthillplatform.runtime.request.Request;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

/**
 * Social service for Anthill platform
 *
 * See https://github.com/anthill-platform/anthill-social
 */
public class SocialService extends Service
{
    public static final String ID = "social";
    public static final String API_VERSION = "0.2";

    private static SocialService instance;
    public static SocialService get() { return instance; }
    private static void set(SocialService service) { instance = service; }

    /**
     * Please note that you should not create an instance of the service yourself,
     * and use SocialService.get() to get existing one instead
     */
    public SocialService(AnthillRuntime runtime, String location)
    {
        super(runtime, location, ID, API_VERSION);

        set(this);
    }

    public interface GroupGetCallback
    {
        void complete(Group group, Status status);
    }

    public interface GroupGetProfileCallback
    {
        void complete(JSONObject profile, boolean participant, Status status);
    }

    public interface GroupGetParticipationCallback
    {
        void complete(Group.Participant participant, boolean owner, Status status);
    }

    public interface GroupUpdateCallback
    {
        void complete(JSONObject updatedProfile, Status status);
    }

    public interface GroupBatchUpdateCallback
    {
        void complete(Map<String, JSONObject> updatedProfiles, Status status);
    }

    public interface GroupUpdateParticipantCallback
    {
        void complete(JSONObject updatedProfile, Status status);
    }

    public interface GroupUpdateParticipantPermissionsCallback
    {
        void complete(Status status);
    }

    public interface GroupJoinCallback
    {
        void complete(Status status);
    }

    public interface GroupUpdateSummaryCallback
    {
        void complete(Status status);
    }

    public interface GroupJoinRequestCallback
    {
        void complete(String key, Status status);
    }

    public interface GroupInviteCallback
    {
        void complete(String key, Status status);
    }

    public interface GroupJoinApproveCallback
    {
        void complete(Status status);
    }

    public interface GroupJoinRejectCallback
    {
        void complete(Status status);
    }

    public interface GroupLeaveCallback
    {
        void complete(Status status);
    }

    public interface GroupKickCallback
    {
        void complete(Status status);
    }

    public interface GroupCreateCallback
    {
        void complete(String id, Status status);
    }

    public interface GroupSearchCallback
    {
        void complete(ArrayList<Group> groups, Status status);
    }

    public static class Group
    {
        private String id;
        private String name;
        private JSONObject profile;
        private JoinMethod joinMethod;
        private int freeMembers;
        private String owner;
        private HashMap<String, Participant> participants;
        private Participant me;
        private MessageService.MessageDestination messageDestination;

        public enum JoinMethod
        {
            free, approve, invite
        }

        public static class Participant
        {
            private JSONObject profile;
            private int role;
            private Set<String> permissions;

            private Participant(JSONObject data)
            {
                this.permissions = new HashSet<String>();
                this.role = data.optInt("role", 0);
                this.profile = data.optJSONObject("profile");

                JSONArray p = data.optJSONArray("permissions");
                if (p != null)
                {
                    for (int i = 0, l = p.length(); i < l; i++)
                    {
                        String permission = p.optString(i);
                        if (permission == null || permission.isEmpty())
                            continue;
                        this.permissions.add(permission);
                    }
                }
            }

            public JSONObject getProfile()
            {
                return profile;
            }

            public int getRole()
            {
                return role;
            }

            public Set<String> getPermissions()
            {
                return permissions;
            }

            public boolean hasPermission(String permission)
            {
                return permissions.contains(permission);
            }
        }

        public Group(JSONObject data)
        {
            this.me = null;
            this.messageDestination = null;

            JSONObject group = data.optJSONObject("group");

            if (group != null)
            {
                this.id = group.optString("group_id");
                this.name = group.optString("name");
                this.profile = group.optJSONObject("profile");
                this.joinMethod = JoinMethod.valueOf(group.optString("join_method", JoinMethod.free.toString()));
                this.freeMembers = group.optInt("free_members", 0);
                this.owner = group.optString("owner", null);
            }

            JSONObject participants = data.optJSONObject("participants");

            if (participants != null)
            {
                this.participants = new HashMap<String, Participant>();

                for (Object account : participants.keySet())
                {
                    JSONObject p = participants.optJSONObject(account.toString());

                    if (p != null)
                    {
                        this.participants.put(account.toString(), new Participant(p));
                    }
                }
            }

            if (data.has("me"))
            {
                JSONObject me = data.optJSONObject("me");

                if (me != null)
                {
                    this.me = new Participant(me);
                }
            }

            if (data.has("message"))
            {
                JSONObject messageDestination = data.optJSONObject("message");
                this.messageDestination = new MessageService.MessageDestination(messageDestination);
            }
        }

        public String getOwner()
        {
            return owner;
        }

        public JSONObject getProfile()
        {
            return profile;
        }

        public int getFreeMembers()
        {
            return freeMembers;
        }

        public boolean isFreeMembersLeft()
        {
            return freeMembers > 0;
        }

        public JoinMethod getJoinMethod()
        {
            return joinMethod;
        }

        public HashMap<String, Participant> getParticipants()
        {
            return participants;
        }

        public Participant getMe()
        {
            return me;
        }

        public String getId()
        {
            return id;
        }

        public String getName()
        {
            return name;
        }
    }

    public void getGroup(String groupId,
                         AccessToken accessToken,
                         final GroupGetCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getRuntime(), getLocation() + "/group/" + groupId,
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                if (status == Status.success)
                {
                    Group group = new Group(((JsonRequest) request).getObject());
                    callback.complete(group, Status.success);
                }
                else
                {
                    callback.complete(null, status);
                }
            }
        });

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.setToken(accessToken);
        jsonRequest.get();
    }

    public void getGroupProfile(String groupId,
                         AccessToken accessToken,
                         final GroupGetProfileCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getRuntime(), getLocation() + "/group/" + groupId + "/profile",
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                if (status == Status.success)
                {
                    JSONObject response = ((JsonRequest) request).getObject();

                    boolean participant = response.optBoolean("participant");
                    JSONObject group = response.optJSONObject("group");

                    if (group == null)
                    {
                        callback.complete(null, false, Status.dataCorrupted);
                        return;
                    }

                    JSONObject profile = group.optJSONObject("profile");
                    callback.complete(profile, participant, Status.success);
                }
                else
                {
                    callback.complete(null, false, status);
                }
            }
        });

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.setToken(accessToken);
        jsonRequest.get();
    }

    public void getMyGroupParticipant(String groupId,
                                      AccessToken accessToken, final GroupGetParticipationCallback callback)
    {
        getGroupParticipant(groupId, "me", accessToken, callback);
    }

    public void getGroupParticipant(String groupId,
                                    String accountId,
                                    AccessToken accessToken, final GroupGetParticipationCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getRuntime(),
            getLocation() + "/group/" + groupId + "/participation/" + accountId,
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                if (status == Status.success)
                {
                    JSONObject response = ((JsonRequest) request).getObject();

                    JSONObject participation = response.optJSONObject("participation");
                    boolean owner = response.optBoolean("owner", false);

                    if (participation != null)
                    {
                        callback.complete(new Group.Participant(participation), owner, Status.success);
                    }
                    else
                    {
                        callback.complete(null, false, Status.dataCorrupted);
                    }
                }
                else
                {
                    callback.complete(null, false, status);
                }
            }
        });

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.setToken(accessToken);
        jsonRequest.get();
    }

    public void updateGroupProfile(String groupId,
                                   JSONObject groupProfile,
                                   AccessToken accessToken, final GroupUpdateCallback profileCallback)
    {
        updateGroupProfile(groupId, groupProfile, null, true, accessToken, profileCallback);
    }

    public void updateGroupProfile(String groupId,
                                   JSONObject groupProfile,
                                   JSONObject notify,
                                   boolean merge,
                                   AccessToken accessToken, final GroupUpdateCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getRuntime(), getLocation() + "/group/" + groupId + "/profile",
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                if (status == Status.success)
                {
                    JSONObject response = ((JsonRequest) request).getObject();

                    JSONObject group = response.optJSONObject("group");
                    if (group != null)
                    {
                        JSONObject groupProfile = group.optJSONObject("profile");

                        if (groupProfile != null)
                        {
                            callback.complete(groupProfile, Status.success);
                            return;
                        }
                    }

                    callback.complete(null, Status.success);
                }
                else
                {
                    callback.complete(null, status);
                }
            }
        });

        Map<String, Object> _options = new HashMap<String, Object>();

        _options.put("profile", groupProfile.toString());
        _options.put("merge", merge ? "true" : "false");
        if (notify != null)
            _options.put("notify", notify.toString());
        _options.put("access_token", accessToken.toString());

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.post(_options);
    }

    public void updateGroupBatchProfiles(
        Map<String, JSONObject> profiles, boolean merge,
        AccessToken accessToken, final GroupBatchUpdateCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getRuntime(), getLocation() + "/groups/profiles",
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                if (status == Status.success)
                {
                    JSONObject response = ((JsonRequest) request).getObject();

                    JSONObject groups = response.optJSONObject("groups");
                    if (groups != null)
                    {
                        Map<String, JSONObject> profiles = new HashMap<String, JSONObject>();

                        for (Object o : groups.keySet())
                        {
                            String groupId = o.toString();

                            JSONObject group = groups.optJSONObject(groupId);
                            if (group == null)
                                continue;

                            JSONObject profile = group.optJSONObject("profile");
                            if (profile == null)
                                continue;

                            profiles.put(groupId, profile);
                        }

                        callback.complete(profiles, Status.success);
                        return;
                    }

                    callback.complete(null, Status.success);
                }
                else
                {
                    callback.complete(null, status);
                }
            }
        });

        Map<String, Object> _options = new HashMap<String, Object>();

        JSONObject _profiles = new JSONObject();

        for (String groupId : profiles.keySet())
        {
            _profiles.put(groupId, profiles.get(groupId));
        }

        _options.put("profiles", _profiles.toString());
        _options.put("merge", merge ? "true" : "false");
        _options.put("access_token", accessToken.toString());

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.post(_options);
    }

    public void updateGroupSummary(
            String groupId,
            String name,
            Group.JoinMethod joinMethod,
            JSONObject notify,
            AccessToken accessToken, final GroupUpdateSummaryCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getRuntime(), getLocation() + "/group/" + groupId,
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                callback.complete(status);
            }
        });

        Map<String, Object> _options = new HashMap<String, Object>();

        if (name != null)
            _options.put("name", name);
        if (joinMethod != null)
            _options.put("join_method", joinMethod.toString());
        if (notify != null)
            _options.put("notify", notify.toString());
        _options.put("access_token", accessToken.toString());

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.post(_options);
    }

    public void updateMyGroupParticipation(
            String groupId,
            JSONObject participationProfile,
            JSONObject notify,
            boolean merge,
            AccessToken accessToken, final GroupUpdateParticipantCallback callback)
    {
        updateGroupParticipation(groupId, "me", participationProfile, notify, merge, accessToken, callback);
    }

    public void updateGroupParticipation(
            String groupId,
            String accountId,
            JSONObject participationProfile,
            JSONObject notify,
            boolean merge,
            AccessToken accessToken, final GroupUpdateParticipantCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getRuntime(),
            getLocation() + "/group/" + groupId + "/participation/" + accountId,
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                if (status == Status.success)
                {
                    JSONObject response = ((JsonRequest) request).getObject();

                    JSONObject profile = response.optJSONObject("profile");
                    if (profile != null)
                    {
                        callback.complete(profile, Status.success);
                        return;
                    }

                    callback.complete(null, Status.success);
                }
                else
                {
                    callback.complete(null, status);
                }
            }
        });

        Map<String, Object> _options = new HashMap<String, Object>();

        _options.put("profile", participationProfile.toString());
        _options.put("merge", merge ? "true" : "false");
        if (notify != null)
            _options.put("notify", notify.toString());
        _options.put("access_token", accessToken.toString());

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.post(_options);
    }

    public void updateMyGroupParticipationPermissions(
            String groupId,
            Set<String> permissions,
            int role,
            JSONObject notify,
            AccessToken accessToken, final GroupUpdateParticipantPermissionsCallback callback)
    {
        updateGroupParticipationPermissions(groupId, "me", permissions, role, notify, accessToken, callback);
    }

    public void updateGroupParticipationPermissions(
            String groupId,
            String accountId,
            Set<String> permissions,
            int role,
            JSONObject notify,
            AccessToken accessToken, final GroupUpdateParticipantPermissionsCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getRuntime(),
            getLocation() + "/group/" + groupId + "/participation/" + accountId + "/permissions",
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                callback.complete(status);
            }
        });

        Map<String, Object> _options = new HashMap<String, Object>();

        JSONArray p = new JSONArray();
        for (String permission : permissions)
        {
            p.put(permission);
        }
        _options.put("permissions", p.toString());
        _options.put("role", String.valueOf(role));
        if (notify != null)
            _options.put("notify", notify.toString());
        _options.put("access_token", accessToken.toString());

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.post(_options);
    }

    public void createGroup(String name,
                            Group.JoinMethod joinMethod,
                            int maxMembers,
                            JSONObject groupProfile,
                            JSONObject myParticipationProfile,
                            boolean enableInGroupMessages,
                            AccessToken accessToken, final GroupCreateCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getRuntime(), getLocation() + "/groups/create",
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                if (status == Status.success)
                {
                    JSONObject response = ((JsonRequest) request).getObject();

                    String id = response.optString("id");
                    if (id != null)
                    {
                        callback.complete(id, Status.success);
                        return;
                    }

                    callback.complete(null, Status.dataCorrupted);
                }
                else
                {
                    callback.complete(null, status);
                }
            }
        });

        Map<String, Object> _options = new HashMap<String, Object>();

        _options.put("name", name);
        _options.put("group_profile", groupProfile.toString());
        if (myParticipationProfile != null)
            _options.put("participation_profile", myParticipationProfile.toString());
        _options.put("join_method", joinMethod.toString());
        _options.put("group_messages", enableInGroupMessages ? "true" : "false");
        _options.put("max_members", String.valueOf(maxMembers));
        _options.put("access_token", accessToken.toString());

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.post(_options);
    }

    public void searchGroups(String query,
                             AccessToken accessToken, final GroupSearchCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getRuntime(), getLocation() + "/groups/search",
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                if (status == Status.success)
                {
                    JSONObject response = ((JsonRequest) request).getObject();

                    JSONArray groups = response.optJSONArray("groups");
                    if (groups != null)
                    {
                        ArrayList<Group> out = new ArrayList<Group>();

                        for (int i = 0, t = groups.length(); i < t; i++)
                        {
                            JSONObject g = groups.optJSONObject(i);

                            if (g != null)
                            {
                                out.add(new Group(g));
                            }
                        }

                        callback.complete(out, Status.success);
                        return;
                    }

                    callback.complete(null, Status.dataCorrupted);
                }
                else
                {
                    callback.complete(null, status);
                }
            }
        });

        Map<String, String> arguments = new HashMap<String, String>();
        arguments.put("query", query);

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.setQueryArguments(arguments);
        jsonRequest.setToken(accessToken);
        jsonRequest.get();
    }

    public void joinGroup(String groupId,
                          AccessToken accessToken, final GroupJoinCallback callback)
    {
        joinGroup(groupId, null, null, accessToken, callback);
    }

    public void joinGroup(String groupId,
                          JSONObject participationProfile,
                          JSONObject notify,
                          AccessToken accessToken, final GroupJoinCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getRuntime(), getLocation() + "/group/" + groupId + "/join",
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                callback.complete(status);
            }
        });

        Map<String, Object> _options = new HashMap<String, Object>();

        if (participationProfile != null)
            _options.put("participation_profile", participationProfile.toString());
        if (notify != null)
            _options.put("notify", notify.toString());
        _options.put("access_token", accessToken.toString());

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.post(_options);
    }

    public void acceptGroupInvitation(
        String groupId, JSONObject participationProfile, JSONObject notify, String key,
        AccessToken accessToken, final GroupJoinCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getRuntime(),
            getLocation() + "/group/" + groupId + "/invitation/accept",
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                callback.complete(status);
            }
        });

        Map<String, Object> _options = new HashMap<String, Object>();

        _options.put("participation_profile", participationProfile.toString());
        if (notify != null)
            _options.put("notify", notify.toString());
        _options.put("key", key);
        _options.put("access_token", accessToken.toString());

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.post(_options);
    }

    public void rejectGroupInvitation(
        String groupId, JSONObject notify, String key,
        AccessToken accessToken, final GroupJoinCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getRuntime(),
            getLocation() + "/group/" + groupId + "/invitation/reject",
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                callback.complete(status);
            }
        });

        Map<String, Object> _options = new HashMap<String, Object>();

        if (notify != null)
            _options.put("notify", notify.toString());

        _options.put("key", key);
        _options.put("access_token", accessToken.toString());

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.post(_options);
    }

    public void leaveGroup(String groupId,
                          AccessToken accessToken, final GroupLeaveCallback callback)
    {
        leaveGroup(groupId, null, accessToken, callback);
    }

    public void leaveGroup(String groupId,
                          JSONObject notify,
                          AccessToken accessToken, final GroupLeaveCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getRuntime(), getLocation() + "/group/" + groupId + "/leave",
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                callback.complete(status);
            }
        });

        Map<String, Object> _options = new HashMap<String, Object>();

        if (notify != null)
            _options.put("notify", notify.toString());
        _options.put("access_token", accessToken.toString());

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.post(_options);
    }

    public void kickFromGroup(String groupId, String accountId,
                              AccessToken accessToken, final GroupKickCallback callback)
    {
        kickFromGroup(groupId, accountId, null, accessToken, callback);
    }

    public void kickFromGroup(String groupId, String accountId,
                              JSONObject notify,
                              AccessToken accessToken, final GroupKickCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getRuntime(),
                getLocation() + "/group/" + groupId + "/participation/" + accountId,
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                callback.complete(status);
            }
        });

        Map<String, Object> _options = new HashMap<String, Object>();

        if (notify != null)
            _options.put("notify", notify.toString());

        _options.put("access_token", accessToken.toString());

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.delete(_options);
    }

    public void transferOwnership(String groupId,
                                  String accountTransferTo, int myNewRole,
                                  JSONObject notify,
                                  AccessToken accessToken, final GroupJoinCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getRuntime(), getLocation() + "/group/" + groupId + "/ownership",
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                callback.complete(status);
            }
        });

        Map<String, Object> _options = new HashMap<String, Object>();

        if (notify != null)
            _options.put("notify", notify.toString());

        _options.put("account_transfer_to", accountTransferTo);
        _options.put("my_role", String.valueOf(myNewRole));
        _options.put("access_token", accessToken.toString());

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.post(_options);
    }

    public void requestJoinGroup(String groupId,
                                 JSONObject participationProfile,
                                 JSONObject notify,
                                 AccessToken accessToken, final GroupJoinRequestCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getRuntime(), getLocation() + "/group/" + groupId + "/request",
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                if (status == Status.success)
                {
                    JSONObject response = ((JsonRequest) request).getObject();

                    String key = response.optString("key");
                    callback.complete(key, Status.success);
                }
                else
                {
                    callback.complete(null, status);
                }
            }
        });

        Map<String, Object> _options = new HashMap<String, Object>();

        if (participationProfile != null)
            _options.put("participation_profile", participationProfile.toString());
        if (notify != null)
            _options.put("notify", notify.toString());
        _options.put("access_token", accessToken.toString());

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.post(_options);
    }

    public void inviteToGroup(String groupId,
                              String accountId,
                              int role,
                              AccessToken accessToken, final GroupInviteCallback callback)
    {
        inviteToGroup(groupId, accountId, role, null, null, accessToken, callback);
    }

    public void inviteToGroup(String groupId,
                              String accountId,
                              int role,
                              Set<String> permissions,
                              JSONObject notify,
                              AccessToken accessToken, final GroupInviteCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getRuntime(),
            getLocation() + "/group/" + groupId + "/invite/" + accountId,
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                if (status == Status.success)
                {
                    JSONObject response = ((JsonRequest) request).getObject();

                    String key = response.optString("key");
                    if (key != null)
                    {
                        callback.complete(key, Status.success);
                        return;
                    }

                    callback.complete(null, Status.success);
                }
                else
                {
                    callback.complete(null, status);
                }
            }
        });

        Map<String, Object> _options = new HashMap<String, Object>();

        _options.put("role", String.valueOf(role));
        if (permissions != null)
        {
            JSONArray p = new JSONArray();
            for (String permission : permissions)
            {
                p.put(permission);
            }
            _options.put("permissions", p.toString());
        }
        else
        {
            _options.put("permissions", "[]");
        }
        if (notify != null)
            _options.put("notify", notify.toString());

        _options.put("access_token", accessToken.toString());

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.post(_options);
    }

    public void approveJoin(String groupId,
                            String accountId,
                            String key,
                            int role,
                            Set<String> permissions,
                            JSONObject notify,
                            AccessToken accessToken, final GroupJoinApproveCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getRuntime(),
            getLocation() + "/group/" + groupId + "/approve/" + accountId,
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                callback.complete(status);
            }
        });

        Map<String, Object> _options = new HashMap<String, Object>();

        _options.put("role", String.valueOf(role));
        if (permissions != null)
        {
            JSONArray p = new JSONArray();
            for (String permission : permissions)
            {
                p.put(permission);
            }
            _options.put("permissions", p.toString());
        }
        else
        {
            _options.put("permissions", "[]");
        }

        if (notify != null)
            _options.put("notify", notify.toString());

        _options.put("key", key);
        _options.put("access_token", accessToken.toString());

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.post(_options);
    }

    public void rejectJoin(String groupId,
                           String accountId,
                           String key,
                           JSONObject notify,
                           AccessToken accessToken, final GroupJoinApproveCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getRuntime(),
            getLocation() + "/group/" + groupId + "/reject/" + accountId,
            new Request.RequestResult()
        {
            @Override
            public void complete(Request request, Status status)
            {
                callback.complete(status);
            }
        });

        Map<String, Object> _options = new HashMap<String, Object>();

        if (notify != null)
            _options.put("notify", notify.toString());

        _options.put("key", key);
        _options.put("access_token", accessToken.toString());

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.post(_options);
    }
}