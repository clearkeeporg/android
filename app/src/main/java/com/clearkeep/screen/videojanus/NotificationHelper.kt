package com.clearkeep.screen.videojanus

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC
import com.clearkeep.screen.chat.room.RoomActivity
import com.clearkeep.utilities.*
import androidx.core.app.Person
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.NotificationTarget
import com.clearkeep.R
import com.clearkeep.db.clear_keep.model.ChatGroup
import com.clearkeep.db.clear_keep.model.Message
import com.clearkeep.db.clear_keep.model.User
import com.clearkeep.db.clear_keep.model.UserPreference
import com.clearkeep.screen.chat.home.MainActivity

const val HEADS_UP_APPEAR_DURATION: Long = 3 * 1000

fun showMessagingStyleNotification(
    context: Context,
    chatGroup: ChatGroup,
    message: Message,
    preference: UserPreference
) {
    val sender = chatGroup.clientList.find { it.userId == message.senderId } ?: User(userId = message.senderId, userName = "unknown", domain = "")
    showHeadsUpMessageWithNoAutoLaunch(context, sender, message, preference)
}

private fun showHeadsUpMessageWithNoAutoLaunch(
    context: Context,
    sender: User,
    message: Message,
    preference: UserPreference
) {
    if (!preference.doNotDisturb) {
        val channelId = MESSAGE_HEADS_UP_CHANNEL_ID
        val channelName = MESSAGE_HEADS_UP_CHANNEL_NAME
        val notificationId = message.createdTime.toInt()

        val intent = Intent(context, RoomActivity::class.java)
        intent.putExtra(RoomActivity.GROUP_ID, message.groupId)
        intent.putExtra(RoomActivity.DOMAIN, message.ownerDomain)
        intent.putExtra(RoomActivity.CLIENT_ID, message.ownerClientId)
        intent.putExtra(RoomActivity.CLEAR_TEMP_MESSAGE, true)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val pendingIntent =
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val trickIntent = Intent()
        val trickPendingIntent =
            PendingIntent.getActivity(context, 0, trickIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val dismissIntent = Intent(context, DismissNotificationReceiver::class.java)
        dismissIntent.action = ACTION_MESSAGE_CANCEL
        dismissIntent.putExtra(MESSAGE_HEADS_UP_CANCEL_NOTIFICATION_ID, notificationId)
        val pendingDismissIntent =
            PendingIntent.getBroadcast(context, 0, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val showSummaryIntent = Intent(context, ShowSummaryNotificationReceiver::class.java)
        showSummaryIntent.putExtra(EXTRA_GROUP_ID, message.groupId)
        showSummaryIntent.putExtra(EXTRA_OWNER_CLIENT, message.ownerClientId)
        showSummaryIntent.putExtra(EXTRA_OWNER_DOMAIN, message.ownerDomain)
        val pendingShowSummaryIntent = PendingIntent.getBroadcast(
            context,
            0,
            showSummaryIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val smallLayout = RemoteViews(context.packageName, R.layout.notification_message_view_small)
        val headsUpLayout =
            RemoteViews(context.packageName, R.layout.notification_message_view_expand)
        val messageContent =
            if (preference.showNotificationPreview) message.message else "You have new message"
        val messageFrom =
            if (preference.showNotificationPreview) "New message from ${sender.userName}" else ""
        smallLayout.apply {
            setTextViewText(R.id.tvFrom, messageFrom)
            setTextViewText(R.id.tvMessage, messageContent)
            setViewVisibility(R.id.imageButton, if (preference.showNotificationPreview) View.VISIBLE else View.GONE)
        }
        headsUpLayout.apply {
            setTextViewText(R.id.tvFrom, messageFrom)
            setTextViewText(R.id.tvMessage, messageContent)
            setViewVisibility(R.id.imageButton, if (preference.showNotificationPreview) View.VISIBLE else View.GONE)
            setOnClickPendingIntent(R.id.tvMute, pendingDismissIntent)
            setOnClickPendingIntent(R.id.tvReply, pendingIntent)
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val newNotificationChannel = NotificationChannel(
                channelId,
                channelName,
                if (preference.notificationSoundVibrate) NotificationManager.IMPORTANCE_MAX else NotificationManager.IMPORTANCE_LOW
            )
            var channel = notificationManager.getNotificationChannel(channelId)
                ?: newNotificationChannel
            val shouldResetChannelImportance = !(channel.importance == NotificationManager.IMPORTANCE_MAX && preference.notificationSoundVibrate || channel.importance == NotificationManager.IMPORTANCE_LOW && !preference.notificationSoundVibrate)
            if (shouldResetChannelImportance) {
                notificationManager.deleteNotificationChannel(channelId)
                channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                notificationManager.createNotificationChannel(newNotificationChannel)
                printlnCK("showHeadsUpMessageWithNoAutoLaunch reset channel importance")
            } else {
                channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                notificationManager.createNotificationChannel(channel)
                printlnCK("showHeadsUpMessageWithNoAutoLaunch keep old channel")
            }
            printlnCK("showHeadsUpMessageWithNoAutoLaunch importance ${channel.importance}")
        }

        val builder: NotificationCompat.Builder = NotificationCompat.Builder(context, channelId)
        val notificationBuilder = builder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(smallLayout)
            .setCustomBigContentView(smallLayout)
            .setCustomHeadsUpContentView(headsUpLayout)
            .setDeleteIntent(pendingShowSummaryIntent)
            .setSmallIcon(R.drawable.ic_logo)
            .setColor(
                ContextCompat.getColor(
                    context.applicationContext,
                    R.color.primaryDefault
                )
            )
            .setFullScreenIntent(trickPendingIntent, true) // trick here to not auto launch activity
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(true)
            .setTimeoutAfter(HEADS_UP_APPEAR_DURATION)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(VISIBILITY_PUBLIC)

        if (preference.notificationSoundVibrate) notificationBuilder.setVibrate(
            longArrayOf(
                Notification.DEFAULT_VIBRATE.toLong()
            )
        )
        val notification = notificationBuilder.build()

        if (preference.showNotificationPreview) {
            val target = NotificationTarget(
                context,
                R.id.imageButton,
                headsUpLayout,
                notification,
                notificationId
            )
            Glide.with(context.applicationContext)
                .asBitmap()
                .circleCrop()
                .load("https://i.ibb.co/WBKb3zf/Thumbnail.png")
                .into(target)
        }

        notificationManager.notify(notificationId, notification)
    }
}

fun showMessageNotificationToSystemBar(
    context: Context,
    me: User,
    chatGroup: ChatGroup,
    messages: List<Message>,
    userPreference: UserPreference
) {
    if (userPreference.doNotDisturb) {
        val channelId = MESSAGE_CHANNEL_ID
        val channelName = MESSAGE_CHANNEL_NAME
        val notificationId = chatGroup.groupId.toInt()

        val contentTitle = chatGroup.groupName
        val participants = chatGroup.clientList
        val userName = if (me.userName.isEmpty()) "me" else me.userName
        val messagingStyle: NotificationCompat.MessagingStyle = NotificationCompat.MessagingStyle(
            Person.Builder()
                .setName(userName)
                .setKey(me.userId)
                .build()
        )
            .setConversationTitle(contentTitle)
            .setGroupConversation(chatGroup.isGroup())

        for (message in messages) {
            val people = participants.find { it.userId == message.senderId }
            messagingStyle.addMessage(
                NotificationCompat.MessagingStyle.Message(
                    message.message,
                    message.createdTime,
                    Person.Builder().setName(people?.userName ?: "unknown").setKey(message.senderId)
                        .build()
                )
            )
        }

        // 3. Set up main Intent for notification.
        val notifyIntent = Intent(context, RoomActivity::class.java)
        notifyIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        notifyIntent.putExtra(RoomActivity.GROUP_ID, chatGroup.groupId)
        notifyIntent.putExtra(RoomActivity.DOMAIN, chatGroup.ownerDomain)
        notifyIntent.putExtra(RoomActivity.CLIENT_ID, chatGroup.ownerClientId)

        val stackBuilder: TaskStackBuilder = TaskStackBuilder.create(context)
        // Adds the back stack
        stackBuilder.addParentStack(MainActivity::class.java)
        // Adds the Intent to the top of the stack
        stackBuilder.addNextIntent(notifyIntent)
        // Gets a PendingIntent containing the entire back stack
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            notifyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var mChannel = notificationManager.getNotificationChannel(channelId)
            if (mChannel == null) {
                mChannel = createMessageToSystemBarNotificationChannel(
                    channelId,
                    channelName,
                    userPreference,
                )
            } else {
                val shouldResetChannelImportance = !(mChannel.importance == NotificationManager.IMPORTANCE_DEFAULT && userPreference.notificationSoundVibrate || mChannel.importance == NotificationManager.IMPORTANCE_LOW && !userPreference.notificationSoundVibrate)
                if (shouldResetChannelImportance) {
                    notificationManager.deleteNotificationChannel(channelId)
                    mChannel = createMessageToSystemBarNotificationChannel(
                        channelId, channelName, userPreference
                    )
                }
            }
            printlnCK("showMessageNotificationToSystemBar importance ${mChannel.importance}")
        }

        val builder: NotificationCompat.Builder = NotificationCompat.Builder(context, channelId)
        builder // MESSAGING_STYLE sets title and content for API 16 and above devices.
            .setStyle(messagingStyle) // Title for API < 16 devices.
            .setContentTitle(contentTitle) // Content for API < 16 devices.
            .setContentTitle("${messages.size} new messages with " + me.userName)
            .setContentText("new messages")
            .setSmallIcon(R.drawable.ic_logo)
            .setContentIntent(mainPendingIntent)
            .setColor(
                ContextCompat.getColor(
                    context.applicationContext,
                    R.color.primaryDefault
                )
            )
            .setGroupSummary(chatGroup.isGroup())
            .setGroup(chatGroup.groupId.toString())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(Notification.CATEGORY_MESSAGE)

        val notification: Notification = builder.build()
        notificationManager.notify(notificationId, notification)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun createMessageToSystemBarNotificationChannel(
    channelId: String,
    channelName: String,
    userPreference: UserPreference
) : NotificationChannel {
    val attributes = AudioAttributes.Builder()
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
        .build()
    val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    return NotificationChannel(
            channelId,
            channelName,
            if (userPreference.notificationSoundVibrate) NotificationManager.IMPORTANCE_DEFAULT else NotificationManager.IMPORTANCE_LOW
        ).apply {
        setSound(notification, attributes)
    }
}