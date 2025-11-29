/**
 * [Mon16:00] Team 02:
 * Haoguang Zhou 1344871
 * Baimin PAN 1329449
 * Yudong Luan 1362030
 */
package ai.strategy;

import ch.aplu.jcardgame.Card;
import core.Rank;
import core.Suit;
import data.GameDataSnapshot;
import protocolframework.Request.BidRequest;
import protocolframework.Request.DecisionRequest;
import protocolframework.decision.BidDecision;
import meld.MeldAnalyzer;

import java.util.*;
import protocolframework.*;


/**
 *SmartBiddingStrategy implements an intelligent bidding strategy based on hand
 *volume and melting point assessment.
 *It determines an appropriate bid amount (or passes).
 *This strategy is restructured based on the original bidding logic of SmartPlayer.
 */
public class SmartBiddingStrategy implements DecisionStrategy<BidDecision>{

    private static final int AGGRESSIVE_SUIT_COUNT = 6;
    private static final int AGGRESSIVE_BID_INCREMENT = 20;
    private static final int DEFAULT_BID_INCREMENT = 10;


    private final MeldAnalyzer meldAnalyzer;

    /**
     Use the splicing analyzer to build strategies to support splicing scoring and suite evaluation.

     * @param meldAnalyzer component, providing weld seam values and weld seam strength estimates
     */
    public SmartBiddingStrategy(MeldAnalyzer meldAnalyzer){
        this.meldAnalyzer = meldAnalyzer;
    }

    /**
     Decide whether to bid or not based on the current game environment.
     Calculate the bidding amount by analyzing the content of the hand cards and the inferred ace cards.

     @param request AI's bid decision request
      * @return a BidDecision representing a selected bid or pass with trump recommendation
     */
    @Override
    public BidDecision decide(DecisionRequest<BidDecision> request){
        BidRequest bidRequest = (BidRequest) request;
        GameDataSnapshot snapshot = request.getSnapshot();

        List<Card> hand = snapshot.getPlayerHand(bidRequest.getPlayerIndex());
        if (hand.isEmpty()) {
            return new BidDecision(true, 0, "C");
        }

        // Determine most common trump suit in hand
        String assumedTrumpSuit = getCurrentAssumedTrumpSuit(hand);

        // Compute meld score
        int meldScore = meldAnalyzer.calculateBestScore(hand, assumedTrumpSuit);

        // If this is the first bid, use meld to initiate
        if (bidRequest.isFirstBid()) {
            return new BidDecision(false, meldScore, assumedTrumpSuit);
        }

        // Respond to current bid with conservative increment
        int bidIncrement = calculateBidIncrement(hand);
        int newBid = bidRequest.getCurrentBid() + bidIncrement;

        // Calculate the bidding threshold by combining meld score and high-card suit value
        int threshold = meldScore + Math.max(
                calculateMajorSuitValue(hand),
                calculateAceKingTenValue(hand)
        );

        boolean shouldPass = newBid >= threshold;

        if (shouldPass) {
            return new BidDecision(true, 0, assumedTrumpSuit);
        } else {
            return new BidDecision(false, bidIncrement, assumedTrumpSuit);
        }
    }

    @Override
    public DecisionType getSupportedType() {
        return DecisionType.BID;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    private int calculateBidIncrement(List<Card> hand){
        Map<String, Integer> suitCounts = new HashMap<>();

        for (Card card:hand) {
            Suit suit =(Suit)card.getSuit();
            String suitName =suit.getSuitShortHand();
            suitCounts.put(suitName, suitCounts.getOrDefault(suitName, 0) + 1);
        }

        // Bid more aggressively if there are 6 or more cards of the same suit
        for (int count:suitCounts.values()){
            if (count >= AGGRESSIVE_SUIT_COUNT) {
                return AGGRESSIVE_BID_INCREMENT;
            }
        }
        return DEFAULT_BID_INCREMENT;
    }

    private int calculateMajorSuitValue(List<Card> hand){
        Map<String, List<Card>> suitGroups = groupCardsBySuit(hand);

        int maxValue = 0;
        for (List<Card> suitCards : suitGroups.values()){
            int suitValue = suitCards.stream()
                    .mapToInt(card -> ((Rank) card.getRank()).getScoreValue())
                    .sum();
            maxValue = Math.max(maxValue, suitValue);
        }

        return maxValue;
    }

    private int calculateAceKingTenValue(List<Card> hand){
        Map<String, List<Card>> suitGroups = groupCardsBySuit(hand);

        int maxValue = 0;
        for (List<Card> suitCards : suitGroups.values()){
            int aceKingTenCount = 0;
            int suitValue = 0;

            for (Card card : suitCards) {
                Rank rank = (Rank) card.getRank();
                int rankValue = rank.getRankCardValue();

                if (rankValue == Rank.ACE.getRankCardValue()||
                        rankValue == Rank.TEN.getRankCardValue()||
                        rankValue == Rank.KING.getRankCardValue()){
                    aceKingTenCount++;
                }

                suitValue += rank.getScoreValue();
            }

            if (aceKingTenCount > 0) {
                maxValue = Math.max(maxValue, suitValue);
            }
        }

        return maxValue;
    }

    private String getCurrentAssumedTrumpSuit(List<Card> hand){
        Map<String, Integer> suitCounts = new HashMap<>();
        for (Card card : hand) {
            Suit suit = (Suit) card.getSuit();
            String suitName = suit.getSuitShortHand();
            suitCounts.put(suitName, suitCounts.getOrDefault(suitName,0) + 1);
        }

        String maxSuit = null;
        int maxCount = 0;
        List<String> tiedSuits = new ArrayList<>();

        for (Map.Entry<String,Integer> entry:suitCounts.entrySet()){
            if (entry.getValue() > maxCount){
                maxCount = entry.getValue();
                maxSuit = entry.getKey();
                tiedSuits.clear();
                tiedSuits.add(maxSuit);
            } else if (entry.getValue()==maxCount){
                tiedSuits.add(entry.getKey());
            }
        }

        if (tiedSuits.size() >1) {
            Random random = new Random();
            return tiedSuits.get(random.nextInt(tiedSuits.size()));
        }

        return maxSuit != null ? maxSuit : "C";
    }

    private Map<String, List<Card>> groupCardsBySuit(List<Card> hand){
        Map<String, List<Card>> groups =new HashMap<>();

        for (Card card : hand) {
            Suit suit = (Suit)card.getSuit();
            String suitName =suit.getSuitShortHand();
            groups.computeIfAbsent(suitName, k ->new ArrayList<>()).add(card);
        }

        return groups;
    }
}