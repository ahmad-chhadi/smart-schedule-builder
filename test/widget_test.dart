import 'package:flutter_test/flutter_test.dart';
import 'package:smart_schedule_builder/main.dart';

void main() {
  testWidgets('the app opens on the courses page', (tester) async {
    await tester.pumpWidget(const SmartScheduleApp());
    await tester.pumpAndSettle();

    expect(find.text('Smart Schedule Builder'), findsOneWidget);
    expect(find.text('Add Course'), findsOneWidget);
    expect(find.text('No courses yet. Add one above.'), findsOneWidget);
  });
}
