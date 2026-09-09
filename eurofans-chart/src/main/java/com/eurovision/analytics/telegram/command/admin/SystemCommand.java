package com.eurovision.analytics.telegram.command.admin;

import com.eurovision.analytics.connectedaccount.Provider;
import com.eurovision.analytics.deployment.DeploymentBindingVerifier;
import com.eurovision.analytics.deployment.VerifiedDeploymentBinding;
import com.eurovision.analytics.eurovision.ArtistStatus;
import com.eurovision.analytics.eurovision.EurovisionArtistRepository;
import com.eurovision.analytics.listening.ListeningEventRepository;
import com.eurovision.analytics.oauth.ProviderAvailabilityService;
import com.eurovision.analytics.telegram.Command;
import com.eurovision.analytics.telegram.CommandContext;
import com.eurovision.analytics.telegram.MessageSender;
import com.eurovision.analytics.user.UserRepository;
import org.springframework.stereotype.Component;

@Component
public class SystemCommand implements Command {

    private final ProviderAvailabilityService availabilityService;
    private final DeploymentBindingVerifier deploymentBindingVerifier;
    private final UserRepository userRepository;
    private final EurovisionArtistRepository artistRepository;
    private final ListeningEventRepository listeningEventRepository;
    private final MessageSender messageSender;

    public SystemCommand(ProviderAvailabilityService availabilityService,
                          DeploymentBindingVerifier deploymentBindingVerifier,
                          UserRepository userRepository,
                          EurovisionArtistRepository artistRepository,
                          ListeningEventRepository listeningEventRepository,
                          MessageSender messageSender) {
        this.availabilityService = availabilityService;
        this.deploymentBindingVerifier = deploymentBindingVerifier;
        this.userRepository = userRepository;
        this.artistRepository = artistRepository;
        this.listeningEventRepository = listeningEventRepository;
        this.messageSender = messageSender;
    }

    @Override
    public String name() {
        return "system";
    }

    @Override
    public boolean requiresAdmin() {
        return true;
    }

    @Override
    public void handle(CommandContext ctx) {
        VerifiedDeploymentBinding binding = deploymentBindingVerifier.requireVerified();
        StringBuilder sb = new StringBuilder("<b>System status</b>\n\n");
        sb.append("Deployment: ").append(binding.casualMode() ? "CASUAL MODE" : "bound")
                .append(" (env=").append(binding.environment()).append(")\n\n");
        sb.append("<b>Providers</b>\n");
        for (Provider provider : Provider.values()) {
            sb.append("• ").append(provider).append(": ")
                    .append(availabilityService.isConfigured(provider) ? "configured" : "not configured").append('\n');
        }
        sb.append("\n<b>Data</b>\n");
        sb.append("Users: ").append(userRepository.count()).append('\n');
        sb.append("Verified artists: ").append(artistRepository.findByStatusAndActiveTrue(ArtistStatus.VERIFIED).size()).append('\n');
        sb.append("Listening events: ").append(listeningEventRepository.count()).append('\n');
        messageSender.send(ctx.chatId(), sb.toString());
    }
}
