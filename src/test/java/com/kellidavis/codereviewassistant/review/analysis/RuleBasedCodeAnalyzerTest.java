package com.kellidavis.codereviewassistant.review.analysis;

import com.kellidavis.codereviewassistant.review.ReviewCategory;
import com.kellidavis.codereviewassistant.review.ReviewFinding;
import com.kellidavis.codereviewassistant.review.ReviewRequest;
import com.kellidavis.codereviewassistant.review.ReviewSeverity;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedCodeAnalyzerTest {
    private final CodeAnalyzer codeAnalyzer = new RuleBasedCodeAnalyzer();

    @Test
    void analyze_withSystemOutPrintln_returnsFinding(){
        ReviewRequest request = new ReviewRequest(
                "src/main/java/PaymentService.java",
                "Java",
                """
                public class PaymentService {
                    public void processPayment() {
                        System.out.println("Processing payment");
                    }
                }
               """
        );

        List<ReviewFinding> findings = codeAnalyzer.analyze(request);
        assertThat(findings).hasSize(1);

        ReviewFinding finding = findings.get(0);
        assertThat(finding.filePath())
                .isEqualTo("src/main/java/PaymentService.java");
        assertThat(finding.lineNumber()).isEqualTo(3);
        assertThat(finding.category())
                .isEqualTo(ReviewCategory.MAINTAINABILITY);
        assertThat(finding.severity())
                .isEqualTo(ReviewSeverity.LOW);
        assertThat(finding.message())
                .isEqualTo("Avoid System.out.println in application code. Use a logger instead.");
    }

    @Test
    void analyze_withoutSystemOutPrintln_returnsEmptyFindings(){
        ReviewRequest request = new ReviewRequest(
                "src/main/java/PaymentService.java",
                "Java",
                """
                     public class PaymentService {
                        public void processPayment() {
                            process();
                        }
                     }
                     """
        );

        List<ReviewFinding> findings = codeAnalyzer.analyze(request);
        assertThat(findings).isEmpty();
    }

    @Test
    void analyze_withMultipleSystemOutPrintlnStatements_returnsMultipleFindings() {
        ReviewRequest request = new ReviewRequest(
                "src/main/java/PaymentService.java",
                "Java",
                """
                public class PaymentService {
                    System.out.println("First");
                    System.out.println("Second");
                }
                """
        );

        List<ReviewFinding> findings = codeAnalyzer.analyze(request);
        assertThat(findings).hasSize(2);
        assertThat(findings.get(0).lineNumber()).isEqualTo(2);
        assertThat(findings.get(1).lineNumber()).isEqualTo(3);
    }

    @Test
    void analyze_withHardcodedPassword_returnsSecurityFinding(){
        ReviewRequest request = new ReviewRequest(
                "src/main/java/PaymentService.java",
                "Java",
                """
                        public class PaymentService {
                            private String password = "super-secret-password";
                        }
                     """

        );

        List<ReviewFinding> findings = codeAnalyzer.analyze(request);
        assertThat(findings).hasSize(1);

        ReviewFinding finding = findings.get(0);
        assertThat(finding.filePath())
                .isEqualTo("src/main/java/PaymentService.java");
        assertThat(finding.lineNumber()).isEqualTo(2);
        assertThat(finding.category())
                .isEqualTo(ReviewCategory.SECURITY);
        assertThat(finding.severity())
                .isEqualTo(ReviewSeverity.HIGH);
        assertThat(finding.message())
                .isEqualTo("Possible hardcoded secret detected. Store sensitive values in environment variables or a secret manager.");
    }

    @Test
    void analyze_withSecretLoadedFromEnvironment_returnsNoSecurityFinding(){
        ReviewRequest request = new ReviewRequest(
                "src/main/java/PaymentService.java",
                "Java",
                """ 
                      public class PaymentService {
                          private String password = System.getenv("DATABASE_PASSWORD");
                      }
                      """
        );

        List<ReviewFinding> findings = codeAnalyzer.analyze(request);
        assertThat(findings).isEmpty();
    }

    @Test
    void analyze_withMultipleRuleViolations_returnsAllFindings(){
        ReviewRequest request = new ReviewRequest(
                "src/main/java/PaymentService.java",
                "Java",
                """
                        public class PaymentService {
                            private String apiKey = "example-api-key";
                        
                            public void processPayment() {
                                System.out.println("Processing payment");
                            }
                        }
                        """
        );

        List<ReviewFinding> findings = codeAnalyzer.analyze(request);
        assertThat(findings).hasSize(2);

        ReviewFinding securityFinding = findings.get(0);
        ReviewFinding maintainabilityFinding = findings.get(1);

        assertThat(securityFinding.lineNumber()).isEqualTo(2);
        assertThat(securityFinding.category())
                .isEqualTo(ReviewCategory.SECURITY);
        assertThat(securityFinding.severity())
                .isEqualTo(ReviewSeverity.HIGH);

        assertThat(maintainabilityFinding.lineNumber()).isEqualTo(5);
        assertThat(maintainabilityFinding.category())
                .isEqualTo(ReviewCategory.MAINTAINABILITY);
        assertThat(maintainabilityFinding.severity())
                .isEqualTo(ReviewSeverity.LOW);
    }
}
