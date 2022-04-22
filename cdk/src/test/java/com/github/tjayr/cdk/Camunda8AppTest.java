 package com.github.tjayr.cdk;

 import com.github.tjayr.cdk.stacks.ZeebeStack;
 import org.junit.jupiter.api.BeforeEach;
 import software.amazon.awscdk.App;
 import software.amazon.awscdk.Environment;
 import software.amazon.awscdk.StackProps;
 import software.amazon.awscdk.assertions.Template;
 import java.io.IOException;

 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;

 import org.junit.jupiter.api.Test;


 public class Camunda8AppTest {

     private StackProps props;

     @BeforeEach
     public void setup() {
         props = StackProps.builder().env(Environment.builder()
                                             .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
                                             .region(System.getenv("CDK_DEFAULT_REGION"))
                                             .build())
                               .build();
     }


     @Test
     public void testZeebeStack() throws IOException {
         App app = new App();
         ZeebeStack stack = new ZeebeStack(app, "zeebe-stack-test", props);

         Template template = Template.fromStack(stack);


         template.hasResourceProperties("AWS::ECS::Service", new HashMap<String, Object>() {{
             put("ServiceName", "zeebe");
         }});

         template.hasResourceProperties("AWS::EFS::FileSystem", new HashMap<String, Object>() {{
             put("FileSystemTags", List.of(Map.of("name", "zeebe-efs")));
         }});
     }

//     @Test
//     public void testHazelcastZeebeStack() throws IOException {
//         App app = new App();
//         ZeebeStack stack = new ZeebeStack(app, "zeebe-stack-test");
//
//         Template template = Template.fromStack(stack);
//
//         template.hasResourceProperties("AWS::Fargate:Service", new HashMap<String, Number>() {{
//             put("VisibilityTimeout", 300);
//         }});
//
//     }
 }
