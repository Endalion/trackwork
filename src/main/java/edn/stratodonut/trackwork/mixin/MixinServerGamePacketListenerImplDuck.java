package edn.stratodonut.trackwork.mixin;

import edn.stratodonut.trackwork.ducks.MSGPLIDuck;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerGamePacketListenerImpl.class)
public class MixinServerGamePacketListenerImplDuck implements MSGPLIDuck {
    @Shadow
    private int aboveGroundTickCount;

    @Unique
    public void tallyho$setAboveGroundTickCount(int value) {
        this.aboveGroundTickCount = value;
    }
}
