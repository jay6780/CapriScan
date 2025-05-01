package app.example.capriscan;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminAnnouncementAdapter extends RecyclerView.Adapter<AdminAnnouncementAdapter.AnnouncementViewHolder> {

    private List<AdminAnnouncement> announcements;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.getDefault());

    public AdminAnnouncementAdapter(List<AdminAnnouncement> announcements) {
        this.announcements = announcements;
    }

    @NonNull
    @Override
    public AnnouncementViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_announcement, parent, false);
        return new AnnouncementViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AnnouncementViewHolder holder, int position) {
        AdminAnnouncement announcement = announcements.get(position);
        holder.titleTextView.setText(announcement.getTitle());
        holder.contentTextView.setText(announcement.getContent());
        holder.timestampTextView.setText(dateFormat.format(new Date(announcement.getTimestamp())));
    }

    @Override
    public int getItemCount() {
        return announcements.size();
    }

    public void setAnnouncements(List<AdminAnnouncement> announcements) {
        this.announcements = announcements;
        notifyDataSetChanged();
    }

    static class AnnouncementViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView contentTextView;
        TextView timestampTextView;

        AnnouncementViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.announcement_title);
            contentTextView = itemView.findViewById(R.id.announcement_content);
            timestampTextView = itemView.findViewById(R.id.announcement_timestamp);
        }
    }
}
