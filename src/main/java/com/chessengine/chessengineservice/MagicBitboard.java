package com.chessengine.chessengineservice;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static com.chessengine.chessengineservice.Helpers.BitboardHelper.isBitSet;
import static com.chessengine.chessengineservice.Helpers.BitboardHelper.setBit;
import static com.chessengine.chessengineservice.Helpers.BoardHelper.*;

public class MagicBitboard {

    public final int[] rookShift = { 52, 52, 52, 52, 52, 52, 52, 52, 53, 53, 53, 54, 53, 53, 54, 53, 53, 54, 54, 54, 53, 53, 54, 53, 53, 54, 53, 53, 54, 54, 54, 53, 52, 54, 53, 53, 53, 53, 54, 53, 52, 53, 54, 54, 53, 53, 54, 53, 53, 54, 54, 54, 53, 53, 54, 53, 52, 53, 53, 53, 53, 53, 53, 52 };
    public final int[] bishopShift = { 58, 60, 59, 59, 59, 59, 60, 58, 60, 59, 59, 59, 59, 59, 59, 60, 59, 59, 57, 57, 57, 57, 59, 59, 59, 59, 57, 55, 55, 57, 59, 59, 59, 59, 57, 55, 55, 57, 59, 59, 59, 59, 57, 57, 57, 57, 59, 59, 60, 60, 59, 59, 59, 59, 60, 60, 58, 60, 59, 59, 59, 59, 59, 58 };
    public final BigInteger[] rookMagic = { new BigInteger("468374916371625120"), new BigInteger("18428729537625841661"), new BigInteger("2531023729696186408"), new BigInteger("6093370314119450896"), new BigInteger("13830552789156493815"), new BigInteger("16134110446239088507"), new BigInteger("12677615322350354425"), new BigInteger("5404321144167858432"), new BigInteger("2111097758984580"), new BigInteger("18428720740584907710"), new BigInteger("17293734603602787839"), new BigInteger("4938760079889530922"), new BigInteger("7699325603589095390"), new BigInteger("9078693890218258431"), new BigInteger("578149610753690728"), new BigInteger("9496543503900033792"), new BigInteger("1155209038552629657"), new BigInteger("9224076274589515780"), new BigInteger("1835781998207181184"), new BigInteger("509120063316431138"), new BigInteger("16634043024132535807"), new BigInteger("18446673631917146111"), new BigInteger("9623686630121410312"), new BigInteger("4648737361302392899"), new BigInteger("738591182849868645"), new BigInteger("1732936432546219272"), new BigInteger("2400543327507449856"), new BigInteger("5188164365601475096"), new BigInteger("10414575345181196316"), new BigInteger("1162492212166789136"), new BigInteger("9396848738060210946"), new BigInteger("622413200109881612"), new BigInteger("7998357718131801918"), new BigInteger("7719627227008073923"), new BigInteger("16181433497662382080"), new BigInteger("18441958655457754079"), new BigInteger("1267153596645440"), new BigInteger("18446726464209379263"), new BigInteger("1214021438038606600"), new BigInteger("4650128814733526084"), new BigInteger("9656144899867951104"), new BigInteger("18444421868610287615"), new BigInteger("3695311799139303489"), new BigInteger("10597006226145476632"), new BigInteger("18436046904206950398"), new BigInteger("18446726472933277663"), new BigInteger("3458977943764860944"), new BigInteger("39125045590687766"), new BigInteger("9227453435446560384"), new BigInteger("6476955465732358656"), new BigInteger("1270314852531077632"), new BigInteger("2882448553461416064"), new BigInteger("11547238928203796481"), new BigInteger("1856618300822323264"), new BigInteger("2573991788166144"), new BigInteger("4936544992551831040"), new BigInteger("13690941749405253631"), new BigInteger("15852669863439351807"), new BigInteger("18302628748190527413"), new BigInteger("12682135449552027479"), new BigInteger("13830554446930287982"), new BigInteger("18302628782487371519"), new BigInteger("7924083509981736956"), new BigInteger("4734295326018586370") };
    public final BigInteger[] bishopMagic = { new BigInteger("16509839532542417919"), new BigInteger("14391803910955204223"), new BigInteger("1848771770702627364"), new BigInteger("347925068195328958"), new BigInteger("5189277761285652493"), new BigInteger("3750937732777063343"), new BigInteger("18429848470517967340"), new BigInteger("17870072066711748607"), new BigInteger("16715520087474960373"), new BigInteger("2459353627279607168"), new BigInteger("7061705824611107232"), new BigInteger("8089129053103260512"), new BigInteger("7414579821471224013"), new BigInteger("9520647030890121554"), new BigInteger("17142940634164625405"), new BigInteger("9187037984654475102"), new BigInteger("4933695867036173873"), new BigInteger("3035992416931960321"), new BigInteger("15052160563071165696"), new BigInteger("5876081268917084809"), new BigInteger("1153484746652717320"), new BigInteger("6365855841584713735"), new BigInteger("2463646859659644933"), new BigInteger("1453259901463176960"), new BigInteger("9808859429721908488"), new BigInteger("2829141021535244552"), new BigInteger("576619101540319252"), new BigInteger("5804014844877275314"), new BigInteger("4774660099383771136"), new BigInteger("328785038479458864"), new BigInteger("2360590652863023124"), new BigInteger("569550314443282"), new BigInteger("17563974527758635567"), new BigInteger("11698101887533589556"), new BigInteger("5764964460729992192"), new BigInteger("6953579832080335136"), new BigInteger("1318441160687747328"), new BigInteger("8090717009753444376"), new BigInteger("16751172641200572929"), new BigInteger("5558033503209157252"), new BigInteger("17100156536247493656"), new BigInteger("7899286223048400564"), new BigInteger("4845135427956654145"), new BigInteger("2368485888099072"), new BigInteger("2399033289953272320"), new BigInteger("6976678428284034058"), new BigInteger("3134241565013966284"), new BigInteger("8661609558376259840"), new BigInteger("17275805361393991679"), new BigInteger("15391050065516657151"), new BigInteger("11529206229534274423"), new BigInteger("9876416274250600448"), new BigInteger("16432792402597134585"), new BigInteger("11975705497012863580"), new BigInteger("11457135419348969979"), new BigInteger("9763749252098620046"), new BigInteger("16960553411078512574"), new BigInteger("15563877356819111679"), new BigInteger("14994736884583272463"), new BigInteger("9441297368950544394"), new BigInteger("14537646123432199168"), new BigInteger("9888547162215157388"), new BigInteger("18140215579194907366"), new BigInteger("18374682062228545019") };
    final BigInteger maxULong = new BigInteger("18446744073709551616");
    long[] bishopMask;
    long[] rookMask;
    long[][] bishopAttacks;
    long[][] rookAttacks;

    /* Precomputed bitboards where moves for bishops and rooks are already calculated
     A mask is a board representing legal moves available to the piece from a square */
    public MagicBitboard() {
        bishopMask = new long[64];
        rookMask = new long[64];
        bishopAttacks = new long[64][];
        rookAttacks = new long[64][];
        computeMasks();
    }

    public long getSliderAttacks(int square, long blockers, boolean sliding)
    {
        return sliding ? getBishopAttacks(square, blockers) : getRookAttacks(square, blockers);
    }

    public long getRookAttacks(int square, long blockers)
    {
        int key = BigInteger.valueOf(blockers & rookMask[square]).multiply(rookMagic[square]).mod(maxULong).shiftRight(rookShift[square]).intValue();
        return rookAttacks[square][key];
    }

    public long getBishopAttacks(int square, long blockers)
    {
        int key = BigInteger.valueOf(blockers & bishopMask[square]).multiply(bishopMagic[square]).mod(maxULong).shiftRight(bishopShift[square]).intValue();
        return bishopAttacks[square][key];
    }

    private void computeMasks() {
        for (int i = 0; i < 64; i++) {
            bishopMask[i] = calculateMovementMask(i, true);
            rookMask[i] = calculateMovementMask(i, false);
        }
        for (int i = 0; i < 64; i++) {
            bishopAttacks[i] = calculateAttacks(i, true, bishopMagic[i], bishopShift[i]);
            rookAttacks[i] = calculateAttacks(i, false, rookMagic[i], rookShift[i]);
        }
    }

    private long calculateMovementMask(int square, boolean diagonal) {
        long mask = 0;
        Pair<Integer, Integer>[] movement = diagonal ? getBishopMovement() : getRookMovement();
        int x = posToX(square);
        int y = posToY(square);

        for (Pair<Integer, Integer> move : movement) {
            for (int i = 1; i < 8; i++) {
                int newX = x + move.first * i;
                int newY = y + move.second * i;
                int nextX = x + move.first * (i + 1);
                int nextY = y + move.second * (i + 1);

                if (areCoorsValid(nextX, nextY)) {
                    mask = setBit(mask, coorsToPos(newX, newY));
                } else {
                    break;
                }
            }
        }
        return mask;
    }

    private long[] calculateAttacks(int square, boolean diagonal, BigInteger magic, int shift) {
        int bitIndex = 64 - shift;
        int size = 1 << bitIndex;
        long[] table = new long[size];

        long movementMask = calculateMovementMask(square, diagonal);
        long[] blockers = getBlockerBitboards(movementMask);

        for (long blocker : blockers) {
            int index = magic.multiply(BigInteger.valueOf(blocker)).mod(maxULong).shiftRight(shift).intValue();
            long moves = getLegalMoveBitboardFromBlockers(square, blocker, diagonal);
            table[index] = moves;
        }
        return table;
    }

    private long[] getBlockerBitboards(long mask)
    {
        // Create a list of the indices of the bits that are set in the movement mask
        List<Integer> moveSquareIndices = new ArrayList<>();
        for (int i = 0; i < 64; i++)
        {
            if (((mask >> i) & 1) == 1)
            {
                moveSquareIndices.add(i);
            }
        }
        // Calculate total number of different bitboards
        int numBlockers = 1 << moveSquareIndices.size();
        long[] blockerBitboards = new long[numBlockers];

        // Create all bitboards
        for (int blockerIndex = 0; blockerIndex < numBlockers; blockerIndex++)
        {
            for (int bitIndex = 0; bitIndex < moveSquareIndices.size(); bitIndex++)
            {
                int bit = (blockerIndex >> bitIndex) & 1;
                blockerBitboards[blockerIndex] |= (long)bit << moveSquareIndices.get(bitIndex);
            }
        }
        return blockerBitboards;
    }

    public long getLegalMoveBitboardFromBlockers(int startSquare, long blockerBitboard, boolean diagonal)
    {
        long bitboard = 0;
        Pair[] movement = diagonal ? getBishopMovement() : getRookMovement();
        int x = posToX(startSquare);
        int y = posToY(startSquare);

        for (Pair<Integer, Integer> move : movement)
        {
            for (int i = 1; i < 8; i++) {
                int newX = x + move.first * i;
                int newY = y + move.second * i;
                int newPos = coorsToPos(newX, newY);

                if (areCoorsValid(newX, newY)) {
                    bitboard = setBit(bitboard, newPos);
                    if (isBitSet(blockerBitboard, newPos)) {
                        break;
                    }
                } else {
                    break;
                }
            }
        }
        return bitboard;
    }
}