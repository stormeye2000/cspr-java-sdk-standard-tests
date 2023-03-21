package com.stormeye.evaluation;

import static org.junit.jupiter.api.Assertions.*;

import com.casper.sdk.exception.CasperClientException;
import com.casper.sdk.exception.NoSuchTypeException;
import com.casper.sdk.helper.CasperTransferHelper;
import com.casper.sdk.identifier.block.BlockIdentifier;
import com.casper.sdk.identifier.block.HashBlockIdentifier;
import com.casper.sdk.identifier.block.HeightBlockIdentifier;
import com.casper.sdk.model.block.JsonBlockData;
import com.casper.sdk.model.common.Digest;
import com.casper.sdk.model.common.Ttl;
import com.casper.sdk.model.deploy.Deploy;
import com.casper.sdk.model.deploy.DeployData;
import com.casper.sdk.model.deploy.DeployResult;
import com.casper.sdk.model.era.EraInfoData;
import com.casper.sdk.model.key.PublicKey;
import com.casper.sdk.model.transfer.TransferData;
import com.casper.sdk.service.CasperService;
import com.casper.sdk.service.EventService;
import com.syntifi.crypto.key.Ed25519PrivateKey;

import java.io.IOException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;
import java.util.Random;
import dev.oak3.sbs4j.exception.ValueSerializationException;
import io.cucumber.java.BeforeAll;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;

public class EvaluateBlocks {

    private static JsonBlockData blockData;
    private static final String url = "localhost";
    private static final Integer port = 11101;
    protected static CasperService csprServiceNctl;

    protected static EventService eventService;

    private static final String invalidBlockHash = "2fe9630b7790852e4409d815b04ca98f37effcdf9097d317b9b9b8ad658f47c8";
    private static final long invalidHeight = 9999999999L;

    private static final String blockErrorMsg = "block not known";
    private static final String blockErrorCode = "-32001";
    private static CasperClientException csprClientException;

    @BeforeAll
    public static void setUp() throws MalformedURLException {
        csprServiceNctl = CasperService.usingPeer(url, port);
        blockData = null;
    }

    @Given("that the latest block is requested")
    public void thatTheLatestBlockIsRequested() {
        blockData = csprServiceNctl.getBlock();
    }

    @Then("a valid block is returned")
    public void aValidBlockIsReturned() {
        assertNotNull(blockData);
        assertEquals(blockData.getClass(), JsonBlockData.class);
    }

    @And("with a valid hash")
    public void withAValidHash() {
        assertNotNull(blockData.getBlock().getHash());
        assertNotNull(blockData.getBlock().getHash().getDigest());
        assertEquals(blockData.getBlock().getHash().getClass(), Digest.class);
        assertTrue(blockData.getBlock().getHash().isValid());
    }

    @And("with a valid body")
    public void withAValidBody() {
        assertNotNull(blockData.getBlock().getBody());
    }

    @And("with valid headers")
    public void withValidHeaders() {
        final Date date = new Date();

        assertNotNull(blockData.getBlock().getHeader());

        assertNotNull(blockData.getBlock().getHeader().getStateRootHash());
        assertNotNull(blockData.getBlock().getHeader().getBodyHash());
        assertNotNull(blockData.getBlock().getHeader().getParentHash());
        assertNotNull(blockData.getBlock().getHeader().getAccumulatedSeed());
        assertTrue(date.after(blockData.getBlock().getHeader().getTimeStamp()));
        assertNotNull(blockData.getBlock().getHeader().getTimeStamp());
        assertNotNull(blockData.getBlock().getHeader().getProtocolVersion());

        assertTrue(blockData.getBlock().getHeader().getHeight() > 0L);
        assertTrue(blockData.getBlock().getHeader().getEraId() >= 0L);

        assertTrue(blockData.getBlock().getHeader().getStateRootHash().isValid());
        assertTrue(blockData.getBlock().getHeader().getBodyHash().isValid());
        assertTrue(blockData.getBlock().getHeader().getParentHash().isValid());
        assertTrue(blockData.getBlock().getHeader().getAccumulatedSeed().isValid());
    }

    @And("with valid proofs")
    public void withValidProofs() {
        assertNotNull(blockData.getBlock().getProofs());
    }

    @Given("that a block is returned by hash")
    public void thatABlockIsReturnedByHash() {
        final JsonBlockData latestBlock = csprServiceNctl.getBlock();
        final String hash = latestBlock.getBlock().getHash().toString();

        blockData = csprServiceNctl.getBlock(new HashBlockIdentifier(hash));
    }

    @Given("that a block is returned by height {int}")
    public void thatABlockIsReturnedByHeight(long height) {
        blockData = csprServiceNctl.getBlock(new HeightBlockIdentifier(height));
    }


    @Given("that an invalid block hash is requested")
    public void thatAnInvalidBlockHashIsRequested() {
        csprClientException = assertThrows(CasperClientException.class,
                () -> csprServiceNctl.getBlock(new HashBlockIdentifier(invalidBlockHash)));
    }

    @Given("that an invalid block height is requested")
    public void thatAnInvalidBlockHeightIsRequested() {
        csprClientException = assertThrows(CasperClientException.class,
                () -> csprServiceNctl.getBlock(new HeightBlockIdentifier(invalidHeight)));
    }


    @Then("a valid error message is returned")
    public void aValidErrorMessageIsReturned() {
        assertNotNull(csprClientException.getMessage());

        assertTrue(csprClientException.getMessage().toLowerCase().contains(blockErrorMsg));
        assertTrue(csprClientException.getMessage().toLowerCase().contains(blockErrorCode));

    }

    @Given("that a transfer block is requested")
    public void thatATransferBlockIsRequested() throws NoSuchTypeException, GeneralSecurityException, ValueSerializationException, IOException {

        final DeployResult result = doTransfer();

        final DeployData deploy = csprServiceNctl.getDeploy(result.getDeployHash());

        final TransferData blockTransfers = csprServiceNctl.getBlockTransfers(new HashBlockIdentifier(deploy.getExecutionResults().get(0).getBlockHash()));


    }

    @Given("that a block at era switch is requested")
    public void thatABlockAtEraSwitchIsRequested() {

        JsonBlockData block = csprServiceNctl.getBlock();

        while (block.getBlock().getHeader().getEraEnd() == null) {
            block = csprServiceNctl.getBlock(new HashBlockIdentifier(block.getBlock().getHeader().getParentHash().toString()));
        }

        EraInfoData eraInfoBySwitchBlock = csprServiceNctl.getEraInfoBySwitchBlock(new HashBlockIdentifier(block.getBlock().getHash().toString()));


    }


    private DeployResult doTransfer() throws IOException, NoSuchTypeException, GeneralSecurityException, ValueSerializationException {

        final Ed25519PrivateKey user1 = new Ed25519PrivateKey();
        final Ed25519PrivateKey user2 = new Ed25519PrivateKey();

        user1.readPrivateKey("assets/net-1/user-1/secret_key.pem");
        user2.readPrivateKey("assets/net-1/user-2/secret_key.pem");

        long id = Math.abs(new Random().nextInt());
        final Ttl ttl = Ttl
                .builder()
                .ttl("30m")
                .build();

        final Deploy deploy = CasperTransferHelper.buildTransferDeploy(user1, PublicKey.fromAbstractPublicKey(user2.derivePublicKey()),
                BigInteger.valueOf(2500000000L), "casper-net-1",
                id, BigInteger.valueOf(100000000L), 1L, ttl, new Date(),
                new ArrayList<>());

        final DeployResult deployResult = csprServiceNctl.putDeploy(deploy);

        do {
            if (!csprServiceNctl.getDeploy(deployResult.getDeployHash()).getExecutionResults().isEmpty()){
                break;
            }
        } while(true);

        return deployResult;

    }


}
