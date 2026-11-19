using System.Windows;
using System.Windows.Controls;

namespace StationLive.Windows;

public partial class MainWindow : Window
{
    public MainWindow()
    {
        InitializeComponent();
        ConnectionType.SelectionChanged += (_, _) =>
        {
            bool m3u = (ConnectionType.SelectedItem as ComboBoxItem)?.Content?.ToString() == "M3U / M3U8";
            Username.Visibility = Password.Visibility = m3u ? Visibility.Collapsed : Visibility.Visible;
            PlaylistUrl.Visibility = m3u ? Visibility.Visible : Visibility.Collapsed;
        };
    }

    private void Continue_Click(object sender, RoutedEventArgs e)
    {
        bool m3u = (ConnectionType.SelectedItem as ComboBoxItem)?.Content?.ToString() == "M3U / M3U8";
        StatusText.Text = m3u
            ? (string.IsNullOrWhiteSpace(PlaylistUrl.Text) ? "Enter an M3U/M3U8 URL." : "Backend connection is not implemented yet.")
            : (string.IsNullOrWhiteSpace(Username.Text) || string.IsNullOrWhiteSpace(Password.Password)
                ? "Enter your username and password."
                : "Backend connection is not implemented yet.");
    }
}
